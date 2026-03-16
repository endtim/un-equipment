package com.unequipment.platform.modules.finance.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.dto.RechargeRequest;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.finance.repository.RechargeOrderRepository;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.finance.repository.TransactionRecordRepository;
import com.unequipment.platform.modules.finance.vo.FinanceAnomalyVO;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final AccountRepository accountRepository;
    private final RechargeOrderRepository rechargeOrderRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final MessageService messageService;
    private final OperationLogService operationLogService;
    private final SysUserRepository userRepository;
    private final ReservationOrderRepository orderRepository;

    public Map<String, Object> accountInfo(SysUser user) {
        Account account = getAccount(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("id", account.getId());
        result.put("balance", account.getBalance());
        result.put("status", account.getStatus());
        result.put("totalRecharge", account.getTotalRecharge());
        result.put("totalConsume", account.getTotalConsume());
        result.put("pendingRechargeCount", rechargeOrderRepository.countByUserIdAndStatus(user.getId(), "PENDING"));
        return result;
    }

    public PageResponse<TransactionRecord> pageMyTransactions(SysUser user, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNum - 1) * safePageSize;
        List<TransactionRecord> list = transactionRecordRepository.findPageByUserId(user.getId(), offset, safePageSize);
        long total = transactionRecordRepository.countByUserId(user.getId());
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    public PageResponse<RechargeOrder> pageMyRecharges(SysUser user, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNum - 1) * safePageSize;
        List<RechargeOrder> list = rechargeOrderRepository.findPageByUserId(user.getId(), offset, safePageSize);
        long total = rechargeOrderRepository.countByUserId(user.getId());
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    @Transactional
    public RechargeOrder submitRecharge(SysUser user, RechargeRequest request) {
        RechargeOrder order = new RechargeOrder();
        order.setRechargeNo(BizNoGenerator.next("RCG"));
        order.setUserId(user.getId());
        order.setAmount(request.getAmount());
        order.setPayMethod("OFFLINE");
        order.setVoucherUrl(request.getProofUrl());
        order.setStatus("PENDING");
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        rechargeOrderRepository.insert(order);
        operationLogService.save(user, "FINANCE", "SUBMIT_RECHARGE", "recharge:" + order.getRechargeNo());
        return order;
    }

    @Transactional
    public RechargeOrder auditRecharge(Long id, RechargeAuditRequest request, SysUser auditor) {
        RechargeOrder order = rechargeOrderRepository.findById(id);
        if (order == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "充值单不存在");
        }
        if (!canManageRecharge(order, auditor)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权审核该充值单");
        }

        LocalDateTime now = LocalDateTime.now();
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase(Locale.ROOT);
        String targetStatus;
        String targetRemark = order.getRemark();
        if ("APPROVE".equals(action)) {
            targetStatus = "PASS";
        } else if ("REJECT".equals(action)) {
            targetStatus = "REJECT";
            targetRemark = request.getComment();
        } else {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "充值审核动作不合法");
        }

        int changed = rechargeOrderRepository.updateIfPending(
            order.getId(),
            targetStatus,
            targetRemark,
            auditor == null ? null : auditor.getId(),
            now,
            now
        );
        if (changed <= 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "充值单已被处理，请刷新后重试");
        }

        SysUser notifyUser = simpleUser(order.getUserId());
        if ("APPROVE".equals(action)) {
            Account account = getAccount(order.getUserId());
            BigDecimal before = nullSafe(account.getBalance());
            int updated = accountRepository.increaseBalanceForRecharge(account.getId(), order.getAmount(), now);
            if (updated <= 0) {
                throw new BizException(ErrorCodes.BIZ_ERROR, "账户更新失败，请稍后重试");
            }
            recordTransaction(
                order.getUserId(),
                null,
                order.getId(),
                order.getAmount(),
                "RECHARGE",
                "IN",
                before,
                before.add(order.getAmount()),
                "充值审核通过"
            );
            messageService.send(notifyUser, "充值审核通过", "您的充值申请已审核通过。");
        } else {
            messageService.send(notifyUser, "充值审核未通过", "您的充值申请未通过审核。");
        }

        operationLogService.save(
            auditor,
            "FINANCE",
            "AUDIT_RECHARGE",
            "rechargeId:" + id + ":" + targetStatus
        );
        return rechargeOrderRepository.findById(id);
    }

    @Transactional
    public void deductForOrder(ReservationOrder order, SysUser operator) {
        Account account = getAccount(order.getUserId());
        BigDecimal amount = settlementAmount(order);
        BigDecimal frozenAmount = frozenAmount(order);
        BigDecimal before = nullSafe(account.getBalance());
        BigDecimal after = before.subtract(amount);
        int changed = accountRepository.consumeWithFreeze(
            account.getId(),
            amount,
            frozenAmount,
            LocalDateTime.now()
        );
        if (changed <= 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "可用余额不足，无法完成结算");
        }

        recordTransaction(
            order.getUserId(),
            order.getId(),
            null,
            amount.negate(),
            "CONSUME",
            "OUT",
            before,
            after,
            "订单结算扣费"
        );

        int confirmed = settlementRecordRepository.confirmByOrderId(
            order.getId(),
            amount,
            LocalDateTime.now(),
            operator == null ? null : operator.getId()
        );
        if (confirmed <= 0) {
            SettlementRecord exists = settlementRecordRepository.findByOrderId(order.getId());
            if (exists != null) {
                settlementRecordRepository.updateStatusByOrderId(
                    order.getId(),
                    "CONFIRMED",
                    LocalDateTime.now(),
                    operator == null ? null : operator.getId(),
                    amount
                );
            } else {
                SettlementRecord settlement = new SettlementRecord();
                settlement.setSettlementNo(BizNoGenerator.next("SET"));
                settlement.setOrderId(order.getId());
                settlement.setUserId(order.getUserId());
                settlement.setInstrumentId(order.getInstrumentId());
                settlement.setBillType(resolveBillType(order.getUserId()));
                settlement.setPriceDesc("订单自动生成");
                settlement.setEstimatedAmount(nullSafe(order.getEstimatedAmount()));
                settlement.setDiscountAmount(BigDecimal.ZERO);
                settlement.setFinalAmount(amount);
                settlement.setSettleStatus("CONFIRMED");
                settlement.setSettledTime(LocalDateTime.now());
                settlement.setOperatorUserId(operator == null ? null : operator.getId());
                settlement.setCreateTime(LocalDateTime.now());
                settlementRecordRepository.insert(settlement);
            }
        }

        operationLogService.save(
            operator,
            "FINANCE",
            "SETTLE_ORDER",
            "orderId:" + order.getId() + ",before=" + before + ",after=" + after + ",finalAmount=" + amount
        );
    }

    @Transactional
    public void refundForOrder(ReservationOrder order, String reason) {
        refundForOrder(order, reason, null);
    }

    @Transactional
    public void refundForOrder(ReservationOrder order, String reason, SysUser operator) {
        Account account = getAccount(order.getUserId());
        BigDecimal amount = settlementAmount(order);
        BigDecimal before = nullSafe(account.getBalance());
        int changed = accountRepository.increaseBalanceForRefund(account.getId(), amount, LocalDateTime.now());
        if (changed <= 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "退款失败，请稍后重试");
        }
        settlementRecordRepository.updateStatusByOrderId(
            order.getId(),
            "REFUNDED",
            LocalDateTime.now(),
            operator == null ? null : operator.getId(),
            amount
        );
        recordTransaction(
            order.getUserId(),
            order.getId(),
            null,
            amount,
            "REFUND",
            "IN",
            before,
            before.add(amount),
            reason
        );
        operationLogService.save(
            operator,
            "FINANCE",
            "REFUND_ORDER",
            "orderId:" + order.getId() + ",before=" + before + ",after=" + before.add(amount) + ",refund=" + amount
        );
    }

    @Transactional
    public void ensurePendingSettlementRecord(ReservationOrder order) {
        SettlementRecord exists = settlementRecordRepository.findByOrderId(order.getId());
        BigDecimal estimated = nullSafe(order.getEstimatedAmount());
        BigDecimal finalAmount = settlementAmount(order);
        String billType = resolveBillType(order.getUserId());
        if (exists == null) {
            SettlementRecord settlement = new SettlementRecord();
            settlement.setSettlementNo(BizNoGenerator.next("SET"));
            settlement.setOrderId(order.getId());
            settlement.setUserId(order.getUserId());
            settlement.setInstrumentId(order.getInstrumentId());
            settlement.setBillType(billType);
            settlement.setPriceDesc("待结算");
            settlement.setEstimatedAmount(estimated);
            settlement.setDiscountAmount(BigDecimal.ZERO);
            settlement.setFinalAmount(finalAmount);
            settlement.setSettleStatus("PENDING");
            settlement.setCreateTime(LocalDateTime.now());
            settlementRecordRepository.insert(settlement);
            return;
        }
        settlementRecordRepository.updatePendingByOrderId(
            order.getId(),
            billType,
            "待结算",
            estimated,
            BigDecimal.ZERO,
            finalAmount
        );
    }

    @Transactional
    public void freezeForOrder(ReservationOrder order) {
        BigDecimal amount = frozenAmount(order);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Account account = getAccount(order.getUserId());
        int changed = accountRepository.freezeAmountIfAvailable(account.getId(), amount, LocalDateTime.now());
        if (changed <= 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "可用余额不足，无法冻结预估金额");
        }
    }

    @Transactional
    public void releaseFreezeForOrder(ReservationOrder order) {
        BigDecimal amount = frozenAmount(order);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Account account = getAccount(order.getUserId());
        accountRepository.unfreezeAmount(account.getId(), amount, LocalDateTime.now());
    }

    @Transactional
    public void markSettlementVoid(ReservationOrder order, SysUser operator) {
        BigDecimal finalAmount = settlementAmount(order);
        settlementRecordRepository.updateStatusByOrderId(
            order.getId(),
            "VOID",
            null,
            operator == null ? null : operator.getId(),
            finalAmount
        );
    }

    public PageResponse<RechargeOrder> pageRecharges(SysUser requester, String keyword, String status,
                                                     Long userId, Long auditUserId,
                                                     BigDecimal minAmount, BigDecimal maxAmount,
                                                     LocalDateTime startTime, LocalDateTime endTime,
                                                     int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNum - 1) * safePageSize;
        String roleCode = normalizeRole(requester);
        Long scopeDepartmentId = requester == null ? null : requester.getDepartmentId();
        List<RechargeOrder> list = rechargeOrderRepository.findPageByScope(
            trimToNull(keyword),
            trimToNull(status),
            userId,
            auditUserId,
            minAmount,
            maxAmount,
            startTime,
            endTime,
            roleCode,
            scopeDepartmentId,
            offset,
            safePageSize
        );
        long total = rechargeOrderRepository.countPageByScope(
            trimToNull(keyword),
            trimToNull(status),
            userId,
            auditUserId,
            minAmount,
            maxAmount,
            startTime,
            endTime,
            roleCode,
            scopeDepartmentId
        );
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    public String exportRechargesCsv(SysUser requester, String keyword, String status,
                                     Long userId, Long auditUserId,
                                     BigDecimal minAmount, BigDecimal maxAmount,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        List<RechargeOrder> list = pageRecharges(
            requester,
            keyword,
            status,
            userId,
            auditUserId,
            minAmount,
            maxAmount,
            startTime,
            endTime,
            1,
            Integer.MAX_VALUE
        ).getList();
        StringBuilder sb = new StringBuilder();
        sb.append("充值单号,申请人,审核人,金额,状态,申请时间,审核时间,备注\n");
        for (RechargeOrder item : list) {
            sb.append(csv(item.getRechargeNo())).append(",")
                .append(csv(item.getUserName())).append(",")
                .append(csv(item.getAuditUserName())).append(",")
                .append(csv(item.getAmount())).append(",")
                .append(csv(item.getStatus())).append(",")
                .append(csv(item.getCreateTime())).append(",")
                .append(csv(item.getAuditTime())).append(",")
                .append(csv(item.getRemark())).append("\n");
        }
        return sb.toString();
    }

    public Map<String, Object> reconciliationOverview(SysUser requester, LocalDateTime startTime, LocalDateTime endTime) {
        String roleCode = normalizeRole(requester);
        Long scopeDepartmentId = requester == null ? null : requester.getDepartmentId();

        long rechargeCount = safeGet(
            () -> rechargeOrderRepository.countByScopeAndCreateTime(startTime, endTime, roleCode, scopeDepartmentId),
            0L
        );
        BigDecimal rechargeAmount = nullSafe(safeGet(
            () -> rechargeOrderRepository.sumAmountByStatusAndScope("PASS", startTime, endTime, roleCode, scopeDepartmentId),
            BigDecimal.ZERO
        ));
        long settlementCount = safeGet(
            () -> settlementRecordRepository.countByScopeAndCreateTime(startTime, endTime, roleCode, scopeDepartmentId),
            0L
        );
        BigDecimal settledAmount = nullSafe(safeGet(
            () -> settlementRecordRepository.sumFinalAmountByStatusAndScope("CONFIRMED", startTime, endTime, roleCode, scopeDepartmentId),
            BigDecimal.ZERO
        ));
        BigDecimal refundedAmount = nullSafe(safeGet(
            () -> settlementRecordRepository.sumFinalAmountByStatusAndScope("REFUNDED", startTime, endTime, roleCode, scopeDepartmentId),
            BigDecimal.ZERO
        ));
        long completedButUnsettled = safeGet(
            () -> orderRepository.countCompletedButUnsettledByScope(startTime, endTime, roleCode, scopeDepartmentId),
            0L
        );
        long waitingSettlement = safeGet(
            () -> orderRepository.countWaitingSettlementByScope(startTime, endTime, roleCode, scopeDepartmentId),
            0L
        );
        long confirmedButUnpaid = safeGet(
            () -> orderRepository.countConfirmedButUnpaidByScope(startTime, endTime, roleCode, scopeDepartmentId),
            0L
        );

        Map<String, Object> result = new HashMap<>();
        result.put("rechargeCount", rechargeCount);
        result.put("rechargeAmount", rechargeAmount);
        result.put("settlementCount", settlementCount);
        result.put("settledAmount", settledAmount);
        result.put("refundedAmount", refundedAmount);
        result.put("completedButUnsettled", completedButUnsettled);
        result.put("waitingSettlementOrders", waitingSettlement);
        result.put("confirmedButUnpaidOrders", confirmedButUnpaid);
        result.put("rangeStart", startTime);
        result.put("rangeEnd", endTime);
        return result;
    }

    public PageResponse<FinanceAnomalyVO> reconciliationAnomalies(SysUser requester, String type,
                                                                  LocalDateTime startTime, LocalDateTime endTime,
                                                                  int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNum - 1) * safePageSize;
        String roleCode = normalizeRole(requester);
        Long scopeDepartmentId = requester == null ? null : requester.getDepartmentId();
        String anomalyType = trimToNull(type);
        if (anomalyType != null) {
            anomalyType = anomalyType.toUpperCase(Locale.ROOT);
        }
        List<FinanceAnomalyVO> list = orderRepository.findFinanceAnomalyPage(
            anomalyType,
            startTime,
            endTime,
            roleCode,
            scopeDepartmentId,
            offset,
            safePageSize
        );
        long total = orderRepository.countFinanceAnomaly(
            anomalyType,
            startTime,
            endTime,
            roleCode,
            scopeDepartmentId
        );
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    public Account getAccount(SysUser user) {
        return getAccount(user.getId());
    }

    public Account getAccount(Long userId) {
        Account account = accountRepository.findByUserId(userId);
        if (account == null) {
            throw new BizException(ErrorCodes.FINANCE_ACCOUNT_NOT_FOUND, "资金账户不存在");
        }
        return account;
    }

    public void ensureEnoughBalance(SysUser user, BigDecimal amount) {
        if (getAccount(user).getBalance().compareTo(amount) < 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "账户余额不足");
        }
    }

    public void ensureEnoughAvailableBalance(SysUser user, BigDecimal amount) {
        Account account = getAccount(user);
        BigDecimal available = nullSafe(account.getBalance()).subtract(nullSafe(account.getFrozenAmount()));
        if (available.compareTo(amount) < 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "可用余额不足");
        }
    }

    private void recordTransaction(Long userId, Long orderId, Long rechargeId, BigDecimal amount, String txnType,
                                   String inoutType, BigDecimal before, BigDecimal after, String remark) {
        TransactionRecord record = new TransactionRecord();
        record.setTxnNo(BizNoGenerator.next("TXN"));
        record.setUserId(userId);
        record.setOrderId(orderId);
        record.setRechargeId(rechargeId);
        record.setTxnType(txnType);
        record.setInoutType(inoutType);
        record.setAmount(amount);
        record.setBalanceBefore(before);
        record.setBalanceAfter(after);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        transactionRecordRepository.insert(record);
    }

    private BigDecimal settlementAmount(ReservationOrder order) {
        BigDecimal finalAmount = order.getFinalAmount();
        if (finalAmount != null && finalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return finalAmount;
        }
        return nullSafe(order.getEstimatedAmount());
    }

    private BigDecimal frozenAmount(ReservationOrder order) {
        return nullSafe(order.getEstimatedAmount());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRole(SysUser user) {
        if (user == null || user.getPrimaryRoleCode() == null || user.getPrimaryRoleCode().trim().isEmpty()) {
            return "INTERNAL_USER";
        }
        return user.getPrimaryRoleCode().trim().toUpperCase(Locale.ROOT);
    }

    private String resolveBillType(Long userId) {
        SysUser user = userRepository.findById(userId);
        if (user == null) {
            return "INTERNAL";
        }
        if ("EXTERNAL_USER".equalsIgnoreCase(user.getPrimaryRoleCode())
            || "EXTERNAL".equalsIgnoreCase(user.getUserType())) {
            return "EXTERNAL";
        }
        return "INTERNAL";
    }

    private boolean canManageRecharge(RechargeOrder order, SysUser operator) {
        if (operator == null || order == null) {
            return false;
        }
        if (hasRole(operator, "ADMIN")) {
            return true;
        }
        if (hasRole(operator, "DEPT_MANAGER")) {
            SysUser target = userRepository.findById(order.getUserId());
            return target != null
                && target.getDepartmentId() != null
                && target.getDepartmentId().equals(operator.getDepartmentId());
        }
        return false;
    }

    private SysUser simpleUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        return user;
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }

    private <T> T safeGet(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
