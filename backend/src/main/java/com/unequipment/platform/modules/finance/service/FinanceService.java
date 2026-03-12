package com.unequipment.platform.modules.finance.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.dto.RechargeRequest;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import com.unequipment.platform.modules.finance.vo.FinanceAnomalyVO;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.finance.repository.RechargeOrderRepository;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.finance.repository.TransactionRecordRepository;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;
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
        result.put("transactions", transactionRecordRepository.findByUserId(user.getId()));
        result.put("recharges", rechargeOrderRepository.findByUserId(user.getId()));
        return result;
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
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "recharge order not found");
        }
        if (!canManageUserResource(order.getUserId(), auditor)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied for this recharge order");
        }
        LocalDateTime now = LocalDateTime.now();
        String targetStatus;
        String targetRemark = order.getRemark();
        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            targetStatus = "PASS";
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            targetStatus = "REJECT";
            targetRemark = request.getComment();
        } else {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "invalid recharge audit action");
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
            throw new BizException(ErrorCodes.BIZ_ERROR, "recharge order already processed");
        }

        SysUser notifyUser = new SysUser();
        notifyUser.setId(order.getUserId());
        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            Account account = getAccount(order.getUserId());
            BigDecimal before = account.getBalance();
            int updated = accountRepository.increaseBalanceForRecharge(account.getId(), order.getAmount(), now);
            if (updated <= 0) {
                throw new BizException(ErrorCodes.BIZ_ERROR, "account update failed");
            }
            recordTransaction(order.getUserId(), null, order.getId(), order.getAmount(), "RECHARGE", "IN",
                before, before.add(order.getAmount()), "Recharge approved");
            messageService.send(notifyUser, "Recharge approved", "Your recharge request has been approved.");
            operationLogService.save(
                auditor,
                "FINANCE",
                "AUDIT_RECHARGE",
                "rechargeId:" + id + ":" + targetStatus
                    + ",before=" + before
                    + ",after=" + before.add(order.getAmount())
            );
        } else {
            messageService.send(notifyUser, "Recharge rejected", "Your recharge request was rejected.");
            operationLogService.save(auditor, "FINANCE", "AUDIT_RECHARGE", "rechargeId:" + id + ":" + targetStatus);
        }
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
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "insufficient available balance");
        }

        recordTransaction(order.getUserId(), order.getId(), null, amount.negate(), "CONSUME", "OUT",
            before, after, "Order settlement");

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
                settlement.setPriceDesc("Auto generated from reservation order");
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
        Account account = getAccount(order.getUserId());
        BigDecimal amount = settlementAmount(order);
        BigDecimal before = account.getBalance();
        int changed = accountRepository.increaseBalanceForRefund(account.getId(), amount, LocalDateTime.now());
        if (changed <= 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "refund failed");
        }
        settlementRecordRepository.updateStatusByOrderId(order.getId(), "REFUNDED", LocalDateTime.now(), null, amount);
        recordTransaction(order.getUserId(), order.getId(), null, amount, "REFUND", "IN",
            before, before.add(amount), reason);
        operationLogService.save(
            null,
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
            settlement.setPriceDesc("Pending settlement");
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
            "Pending settlement",
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
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "insufficient available balance");
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

    public List<RechargeOrder> listRecharges(SysUser requester) {
        return rechargeOrderRepository.findAll().stream()
            .filter(item -> canManageUserResource(item.getUserId(), requester))
            .collect(Collectors.toList());
    }

    public PageResponse<RechargeOrder> pageRecharges(SysUser requester, String keyword, String status,
                                                     Long userId, Long auditUserId,
                                                     BigDecimal minAmount, BigDecimal maxAmount,
                                                     LocalDateTime startTime, LocalDateTime endTime,
                                                     int pageNum, int pageSize) {
        List<RechargeOrder> filtered = new ArrayList<>(listRecharges(requester));
        if (userId != null) {
            filtered = filtered.stream().filter(item -> userId.equals(item.getUserId())).collect(Collectors.toList());
        }
        if (auditUserId != null) {
            filtered = filtered.stream()
                .filter(item -> auditUserId.equals(item.getAuditUserId()))
                .collect(Collectors.toList());
        }
        if (status != null && !status.trim().isEmpty()) {
            String targetStatus = status.trim().toUpperCase();
            filtered = filtered.stream()
                .filter(item -> item.getStatus() != null && targetStatus.equalsIgnoreCase(item.getStatus()))
                .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String key = keyword.trim().toLowerCase();
            filtered = filtered.stream().filter(item ->
                containsIgnoreCase(item.getRechargeNo(), key)
                    || containsIgnoreCase(item.getUserName(), key)
                    || containsIgnoreCase(item.getRemark(), key))
                .collect(Collectors.toList());
        }
        if (minAmount != null) {
            filtered = filtered.stream()
                .filter(item -> nullSafe(item.getAmount()).compareTo(minAmount) >= 0)
                .collect(Collectors.toList());
        }
        if (maxAmount != null) {
            filtered = filtered.stream()
                .filter(item -> nullSafe(item.getAmount()).compareTo(maxAmount) <= 0)
                .collect(Collectors.toList());
        }
        if (startTime != null) {
            filtered = filtered.stream()
                .filter(item -> item.getCreateTime() != null && !item.getCreateTime().isBefore(startTime))
                .collect(Collectors.toList());
        }
        if (endTime != null) {
            filtered = filtered.stream()
                .filter(item -> item.getCreateTime() != null && !item.getCreateTime().isAfter(endTime))
                .collect(Collectors.toList());
        }

        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int from = (safePageNum - 1) * safePageSize;
        int to = Math.min(from + safePageSize, filtered.size());
        List<RechargeOrder> pageList = from >= filtered.size() ? new ArrayList<>() : filtered.subList(from, to);
        return new PageResponse<>(pageList, filtered.size(), safePageNum, safePageSize);
    }

    public String exportRechargesCsv(SysUser requester, String keyword, String status,
                                     Long userId, Long auditUserId,
                                     BigDecimal minAmount, BigDecimal maxAmount,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        List<RechargeOrder> list = pageRecharges(
            requester, keyword, status, userId, auditUserId,
            minAmount, maxAmount, startTime, endTime, 1, Integer.MAX_VALUE
        ).getList();
        StringBuilder sb = new StringBuilder();
        sb.append("充值单号,申请人,审核人,金额,状态,申请时间,审核时间,备注\n");
        for (RechargeOrder item : list) {
            sb.append(csv(item.getRechargeNo())).append(',')
                .append(csv(item.getUserName())).append(',')
                .append(csv(item.getAuditUserName())).append(',')
                .append(csv(item.getAmount())).append(',')
                .append(csv(item.getStatus())).append(',')
                .append(csv(item.getCreateTime())).append(',')
                .append(csv(item.getAuditTime())).append(',')
                .append(csv(item.getRemark())).append('\n');
        }
        return sb.toString();
    }

    public Map<String, Object> reconciliationOverview(SysUser requester, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = new HashMap<>();

        List<RechargeOrder> rechargeOrders = listRecharges(requester).stream()
            .filter(item -> inRange(item.getCreateTime(), startTime, endTime))
            .collect(Collectors.toList());
        BigDecimal rechargeAmount = rechargeOrders.stream()
            .filter(item -> "PASS".equalsIgnoreCase(item.getStatus()))
            .map(RechargeOrder::getAmount)
            .map(this::nullSafe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReservationOrder> scopedOrders = orderRepository.findAll().stream()
            .filter(item -> canManageUserResource(item.getUserId(), requester))
            .filter(item -> inRange(item.getCreateTime(), startTime, endTime))
            .collect(Collectors.toList());
        List<SettlementRecord> scopedSettlements = settlementRecordRepository.findAll().stream()
            .filter(item -> canManageUserResource(item.getUserId(), requester))
            .filter(item -> inRange(item.getCreateTime(), startTime, endTime))
            .collect(Collectors.toList());
        BigDecimal settledAmount = scopedSettlements.stream()
            .filter(item -> "CONFIRMED".equalsIgnoreCase(item.getSettleStatus()))
            .map(SettlementRecord::getFinalAmount)
            .map(this::nullSafe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundedAmount = scopedSettlements.stream()
            .filter(item -> "REFUNDED".equalsIgnoreCase(item.getSettleStatus()))
            .map(SettlementRecord::getFinalAmount)
            .map(this::nullSafe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedButUnsettled = scopedOrders.stream()
            .filter(item -> "COMPLETED".equalsIgnoreCase(item.getOrderStatus()))
            .filter(item -> !"CONFIRMED".equalsIgnoreCase(item.getSettlementStatus())
                && !"REFUNDED".equalsIgnoreCase(item.getSettlementStatus()))
            .count();
        long waitingSettlement = scopedOrders.stream()
            .filter(item -> "WAITING_SETTLEMENT".equalsIgnoreCase(item.getOrderStatus()))
            .count();
        long confirmedButUnpaid = scopedOrders.stream()
            .filter(item -> "CONFIRMED".equalsIgnoreCase(item.getSettlementStatus()))
            .filter(item -> !"PAID".equalsIgnoreCase(item.getPayStatus()))
            .count();

        result.put("rechargeCount", rechargeOrders.size());
        result.put("rechargeAmount", rechargeAmount);
        result.put("settlementCount", scopedSettlements.size());
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
        List<ReservationOrder> scopedOrders = orderRepository.findAll().stream()
            .filter(item -> canManageUserResource(item.getUserId(), requester))
            .filter(item -> inRange(item.getCreateTime(), startTime, endTime))
            .collect(Collectors.toList());
        Map<Long, SettlementRecord> settlementByOrder = settlementRecordRepository.findAll().stream()
            .filter(item -> canManageUserResource(item.getUserId(), requester))
            .collect(Collectors.toMap(SettlementRecord::getOrderId, item -> item, (a, b) -> a));

        List<FinanceAnomalyVO> anomalies = new ArrayList<>();
        for (ReservationOrder order : scopedOrders) {
            SettlementRecord settlement = settlementByOrder.get(order.getId());

            if ("COMPLETED".equalsIgnoreCase(order.getOrderStatus())
                && (settlement == null || "PENDING".equalsIgnoreCase(settlement.getSettleStatus()))) {
                anomalies.add(buildAnomaly("COMPLETED_UNSETTLED", "已完成未结算", order, settlement,
                    "订单已完成但结算未确认"));
            }
            if ("WAITING_SETTLEMENT".equalsIgnoreCase(order.getOrderStatus())) {
                anomalies.add(buildAnomaly("WAITING_SETTLEMENT", "待结算滞留", order, settlement,
                    "订单长时间处于待结算状态"));
            }
            if (settlement != null && "CONFIRMED".equalsIgnoreCase(settlement.getSettleStatus())
                && !"PAID".equalsIgnoreCase(order.getPayStatus())) {
                anomalies.add(buildAnomaly("CONFIRMED_UNPAID", "已结算未支付", order, settlement,
                    "结算已确认但订单支付状态不是已支付"));
            }
            if ("REFUNDED".equalsIgnoreCase(order.getPayStatus())
                && (settlement == null || !"REFUNDED".equalsIgnoreCase(settlement.getSettleStatus()))) {
                anomalies.add(buildAnomaly("REFUNDED_UNMATCHED", "已退款未冲正", order, settlement,
                    "订单支付已退款，但结算记录未标记为已退款"));
            }
        }

        if (type != null && !type.trim().isEmpty()) {
            String target = type.trim().toUpperCase(Locale.ROOT);
            anomalies = anomalies.stream()
                .filter(item -> target.equalsIgnoreCase(item.getAnomalyType()))
                .collect(Collectors.toList());
        }
        anomalies.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) {
                return 0;
            }
            if (a.getCreateTime() == null) {
                return 1;
            }
            if (b.getCreateTime() == null) {
                return -1;
            }
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int from = (safePageNum - 1) * safePageSize;
        int to = Math.min(from + safePageSize, anomalies.size());
        List<FinanceAnomalyVO> pageList = from >= anomalies.size() ? new ArrayList<>() : anomalies.subList(from, to);
        return new PageResponse<>(pageList, anomalies.size(), safePageNum, safePageSize);
    }

    public Account getAccount(SysUser user) {
        return getAccount(user.getId());
    }

    public Account getAccount(Long userId) {
        Account account = accountRepository.findByUserId(userId);
        if (account == null) {
            throw new BizException(ErrorCodes.FINANCE_ACCOUNT_NOT_FOUND, "account not found");
        }
        return account;
    }

    public void ensureEnoughBalance(SysUser user, BigDecimal amount) {
        if (getAccount(user).getBalance().compareTo(amount) < 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "insufficient balance");
        }
    }

    public void ensureEnoughAvailableBalance(SysUser user, BigDecimal amount) {
        Account account = getAccount(user);
        BigDecimal available = nullSafe(account.getBalance()).subtract(nullSafe(account.getFrozenAmount()));
        if (available.compareTo(amount) < 0) {
            throw new BizException(ErrorCodes.FINANCE_INSUFFICIENT_BALANCE, "insufficient available balance");
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

    private boolean containsIgnoreCase(String value, String keywordLowerCase) {
        return value != null && value.toLowerCase().contains(keywordLowerCase);
    }

    private boolean inRange(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        if (target == null) {
            return false;
        }
        if (start != null && target.isBefore(start)) {
            return false;
        }
        if (end != null && target.isAfter(end)) {
            return false;
        }
        return true;
    }

    private FinanceAnomalyVO buildAnomaly(String type, String label, ReservationOrder order,
                                          SettlementRecord settlement, String detail) {
        FinanceAnomalyVO vo = new FinanceAnomalyVO();
        vo.setAnomalyType(type);
        vo.setAnomalyLabel(label);
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setUserName(order.getUserName());
        vo.setCreateTime(order.getCreateTime());
        vo.setDetail(detail);
        if (settlement != null) {
            vo.setSettlementId(settlement.getId());
            vo.setSettlementNo(settlement.getSettlementNo());
        }
        return vo;
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
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

    private boolean canManageUserResource(Long targetUserId, SysUser operator) {
        if (operator == null) {
            return false;
        }
        if (hasRole(operator, "ADMIN")) {
            return true;
        }
        if (targetUserId != null && targetUserId.equals(operator.getId())) {
            return true;
        }
        if (hasRole(operator, "DEPT_MANAGER")) {
            SysUser target = userRepository.findById(targetUserId);
            return target != null && target.getDepartmentId() != null
                && target.getDepartmentId().equals(operator.getDepartmentId());
        }
        return false;
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }
}
