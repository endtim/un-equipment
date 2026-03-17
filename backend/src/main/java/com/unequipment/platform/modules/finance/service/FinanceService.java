package com.unequipment.platform.modules.finance.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.common.util.RoleAuthUtils;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.dto.RechargeRequest;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.entity.FinanceAnomalyHandle;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.finance.dto.FinanceAnomalyHandleRequest;
import com.unequipment.platform.modules.finance.repository.FinanceAnomalyHandleRepository;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceService {
    private static final int MAX_PAGE_SIZE = 200;
    private static final int EXPORT_MAX_ROWS = 10000;
    private static final long RECONCILIATION_OVERVIEW_CACHE_MILLIS = Duration.ofSeconds(45).toMillis();

    private final AccountRepository accountRepository;
    private final RechargeOrderRepository rechargeOrderRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final MessageService messageService;
    private final OperationLogService operationLogService;
    private final SysUserRepository userRepository;
    private final ReservationOrderRepository orderRepository;
    private final FinanceAnomalyHandleRepository financeAnomalyHandleRepository;
    private final Map<String, CacheEntry> reconciliationOverviewCache = new ConcurrentHashMap<>();

    @Value("${app.finance.recharge-double-review-threshold:5000}")
    private BigDecimal rechargeDoubleReviewThreshold;

    public Map<String, Object> accountInfo(SysUser user) {
        Account account = getAccount(user.getId());
        BigDecimal balance = nullSafe(account.getBalance());
        BigDecimal frozenAmount = nullSafe(account.getFrozenAmount());
        BigDecimal availableBalance = balance.subtract(frozenAmount);
        Map<String, Object> result = new HashMap<>();
        result.put("id", account.getId());
        result.put("balance", balance);
        result.put("frozenAmount", frozenAmount);
        result.put("availableBalance", availableBalance);
        result.put("status", account.getStatus());
        result.put("totalRecharge", account.getTotalRecharge());
        result.put("totalConsume", account.getTotalConsume());
        result.put("pendingRechargeCount", rechargeOrderRepository.countByUserIdAndStatus(user.getId(), "PENDING"));
        return result;
    }

    public PageResponse<TransactionRecord> pageMyTransactions(SysUser user, int pageNum, int pageSize) {
        int safePageNum = sanitizePageNum(pageNum);
        int safePageSize = sanitizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        List<TransactionRecord> list = transactionRecordRepository.findPageByUserId(user.getId(), offset, safePageSize);
        long total = transactionRecordRepository.countByUserId(user.getId());
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    public PageResponse<RechargeOrder> pageMyRecharges(SysUser user, int pageNum, int pageSize) {
        int safePageNum = sanitizePageNum(pageNum);
        int safePageSize = sanitizePageSize(pageSize);
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
        order.setReviewStatus("NONE");
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        rechargeOrderRepository.insert(order);
        clearFinanceCache();
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
        boolean needDoubleReview = needDoubleReview(order);
        String currentStatus = order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase(Locale.ROOT);
        if ("REJECT".equals(action)) {
            if (request.getComment() == null || request.getComment().trim().isEmpty()) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, "驳回原因不能为空");
            }
            targetStatus = "REJECT";
            targetRemark = request.getComment().trim();
            int rejected = rechargeOrderRepository.rejectIfPendingOrReviewPending(
                order.getId(),
                targetStatus,
                "NONE",
                targetRemark,
                auditor == null ? null : auditor.getId(),
                now,
                now
            );
            if (rejected <= 0) {
                throw new BizException(ErrorCodes.BIZ_ERROR, "充值单已被处理，请刷新后重试");
            }
            messageService.send(simpleUser(order.getUserId()), "充值审核未通过", "您的充值申请未通过审核。");
            clearFinanceCache();
            operationLogService.save(
                auditor,
                "FINANCE",
                "AUDIT_RECHARGE",
                "rechargeId:" + id + ":" + targetStatus
            );
            return rechargeOrderRepository.findById(id);
        } else if ("APPROVE".equals(action)) {
            if (needDoubleReview) {
                if ("PENDING".equals(currentStatus)) {
                    int changed = rechargeOrderRepository.updateFirstApproveIfPending(
                        order.getId(),
                        "REVIEW_PENDING",
                        "PENDING",
                        auditor == null ? null : auditor.getId(),
                        now,
                        auditor == null ? null : auditor.getId(),
                        now,
                        now
                    );
                    if (changed <= 0) {
                        throw new BizException(ErrorCodes.BIZ_ERROR, "充值单状态已变化，请刷新后重试");
                    }
                    messageService.send(simpleUser(order.getUserId()), "充值申请进入复核", "您的大额充值申请已进入复核流程。");
                    clearFinanceCache();
                    operationLogService.save(
                        auditor,
                        "FINANCE",
                        "AUDIT_RECHARGE_FIRST_PASS",
                        "rechargeId:" + id + ":REVIEW_PENDING"
                    );
                    return rechargeOrderRepository.findById(id);
                }
                if (!"REVIEW_PENDING".equals(currentStatus)) {
                    throw new BizException(ErrorCodes.BIZ_ERROR, "当前状态不支持通过审核");
                }
                if (auditor != null && order.getFirstAuditUserId() != null
                    && order.getFirstAuditUserId().equals(auditor.getId())) {
                    throw new BizException(ErrorCodes.BIZ_ERROR, "大额充值需双人复核，复核人不能与初审人相同");
                }
                int changed = rechargeOrderRepository.updateSecondApproveIfReviewPending(
                    order.getId(),
                    "PASS",
                    "PASS",
                    auditor == null ? null : auditor.getId(),
                    now,
                    auditor == null ? null : auditor.getId(),
                    now,
                    now
                );
                if (changed <= 0) {
                    throw new BizException(ErrorCodes.BIZ_ERROR, "充值单状态已变化，请刷新后重试");
                }
                targetStatus = "PASS";
            } else {
                int changed = rechargeOrderRepository.updateIfPending(
                    order.getId(),
                    "PASS",
                    targetRemark,
                    auditor == null ? null : auditor.getId(),
                    now,
                    now
                );
                if (changed <= 0) {
                    throw new BizException(ErrorCodes.BIZ_ERROR, "充值单已被处理，请刷新后重试");
                }
                targetStatus = "PASS";
            }
        } else {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "充值审核动作不合法");
        }

        SysUser notifyUser = simpleUser(order.getUserId());
        if ("PASS".equals(targetStatus)) {
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
        }
        clearFinanceCache();

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
        BigDecimal accountFrozen = nullSafe(account.getFrozenAmount());
        BigDecimal releasableFrozen = accountFrozen.min(frozenAmount).min(amount);
        if (releasableFrozen.compareTo(BigDecimal.ZERO) < 0) {
            releasableFrozen = BigDecimal.ZERO;
        }
        BigDecimal before = nullSafe(account.getBalance());
        BigDecimal after = before.subtract(amount);
        int changed = accountRepository.consumeWithFreeze(
            account.getId(),
            amount,
            releasableFrozen,
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
        clearFinanceCache();
    }

    @Transactional
    public void refundForOrder(ReservationOrder order, String reason) {
        refundForOrder(order, reason, null);
    }

    @Transactional
    public void refundForOrder(ReservationOrder order, String reason, SysUser operator) {
        SettlementRecord settlement = settlementRecordRepository.findByOrderId(order.getId());
        if (settlement != null) {
            String settleStatus = settlement.getSettleStatus() == null ? "" : settlement.getSettleStatus().trim().toUpperCase();
            if ("REFUNDED".equals(settleStatus)) {
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "订单已退款，请勿重复操作");
            }
            if ("REFUNDING".equals(settleStatus)) {
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "订单退款处理中，请稍后再试");
            }
        }
        Account account = getAccount(order.getUserId());
        BigDecimal amount = settlementAmount(order);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "退款金额必须大于0");
        }
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
        clearFinanceCache();
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
            clearFinanceCache();
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
        clearFinanceCache();
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
        clearFinanceCache();
    }

    public PageResponse<RechargeOrder> pageRecharges(SysUser requester, String keyword, String status,
                                                     Long userId, Long auditUserId,
                                                     BigDecimal minAmount, BigDecimal maxAmount,
                                                     LocalDateTime startTime, LocalDateTime endTime,
                                                     int pageNum, int pageSize) {
        validateAmountRange(minAmount, maxAmount);
        validateTimeRange(startTime, endTime, "充值申请时间范围不合法，开始时间不能晚于结束时间");
        int safePageNum = sanitizePageNum(pageNum);
        int safePageSize = sanitizePageSize(pageSize);
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
            EXPORT_MAX_ROWS
        ).getList();
        StringBuilder sb = new StringBuilder();
        sb.append("充值单号,申请人,审核人,金额,状态,申请时间,审核时间,备注\n");
        for (RechargeOrder item : list) {
            sb.append(csv(item.getRechargeNo())).append(",")
                .append(csv(item.getUserName())).append(",")
                .append(csv(item.getAuditUserName())).append(",")
                .append(csv(item.getAmount())).append(",")
                .append(csv(rechargeStatusLabel(item.getStatus()))).append(",")
                .append(csv(item.getCreateTime())).append(",")
                .append(csv(item.getAuditTime())).append(",")
                .append(csv(item.getRemark())).append("\n");
        }
        return sb.toString();
    }

    public Map<String, Object> reconciliationOverview(SysUser requester, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime, "对账时间范围不合法，开始时间不能晚于结束时间");
        String cacheKey = buildReconciliationOverviewCacheKey(requester, startTime, endTime);
        CacheEntry cached = reconciliationOverviewCache.get(cacheKey);
        long nowMillis = System.currentTimeMillis();
        if (cached != null && cached.expireAtMillis > nowMillis && cached.value != null) {
            return cached.value;
        }
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
        long rechargePassCount = safeGet(
            () -> rechargeOrderRepository.countByStatusAndScope("PASS", startTime, endTime, roleCode, scopeDepartmentId),
            0L
        );
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
        BigDecimal avgSettleHours = nullSafe(safeGet(
            () -> settlementRecordRepository.avgSettleHoursByScope(startTime, endTime, roleCode, scopeDepartmentId),
            BigDecimal.ZERO
        ));
        BigDecimal avgWaitingSettlementHours = nullSafe(safeGet(
            () -> orderRepository.avgWaitingSettlementHoursByScope(startTime, endTime, roleCode, scopeDepartmentId),
            BigDecimal.ZERO
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("rechargeCount", rechargeCount);
        result.put("rechargePassCount", rechargePassCount);
        result.put("rechargeAmount", rechargeAmount);
        result.put("settlementCount", settlementCount);
        result.put("settledAmount", settledAmount);
        result.put("refundedAmount", refundedAmount);
        result.put("rechargePassRate", calculatePercent(rechargePassCount, rechargeCount));
        result.put("refundRate", calculatePercent(refundedAmount, settledAmount));
        result.put("avgSettleHours", avgSettleHours);
        result.put("avgWaitingSettlementHours", avgWaitingSettlementHours);
        result.put("completedButUnsettled", completedButUnsettled);
        result.put("waitingSettlementOrders", waitingSettlement);
        result.put("confirmedButUnpaidOrders", confirmedButUnpaid);
        result.put("rangeStart", startTime);
        result.put("rangeEnd", endTime);
        reconciliationOverviewCache.put(cacheKey, new CacheEntry(result, nowMillis + RECONCILIATION_OVERVIEW_CACHE_MILLIS));
        return result;
    }

    public PageResponse<FinanceAnomalyVO> reconciliationAnomalies(SysUser requester, String type,
                                                                  LocalDateTime startTime, LocalDateTime endTime,
                                                                  int pageNum, int pageSize) {
        validateTimeRange(startTime, endTime, "对账时间范围不合法，开始时间不能晚于结束时间");
        int safePageNum = sanitizePageNum(pageNum);
        int safePageSize = sanitizePageSize(pageSize);
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
        for (FinanceAnomalyVO item : list) {
            fillAnomalyHandleInfo(item);
        }
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    @Transactional
    public void handleReconciliationAnomaly(SysUser requester, FinanceAnomalyHandleRequest request) {
        if (requester == null || (!hasRole(requester, "ADMIN") && !hasRole(requester, "DEPT_MANAGER"))) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权处理异常账");
        }
        String anomalyType = request.getAnomalyType().trim().toUpperCase(Locale.ROOT);
        Long orderId = request.getOrderId();
        LocalDateTime now = LocalDateTime.now();
        financeAnomalyHandleRepository.upsert(
            anomalyType,
            orderId,
            request.getSettlementId(),
            request.getHandleStatus(),
            trimToNull(request.getHandleComment()),
            requester.getId(),
            now,
            now,
            now
        );
        operationLogService.save(
            requester,
            "FINANCE",
            "HANDLE_RECONCILIATION_ANOMALY",
            "type:" + anomalyType + ",orderId:" + orderId + ",status:" + request.getHandleStatus()
        );
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
        if (hasRole(user, "ADMIN")) {
            return "ADMIN";
        }
        if (hasRole(user, "DEPT_MANAGER")) {
            return "DEPT_MANAGER";
        }
        return user == null || user.getPrimaryRoleCode() == null || user.getPrimaryRoleCode().trim().isEmpty()
            ? "INTERNAL_USER"
            : user.getPrimaryRoleCode().trim().toUpperCase(Locale.ROOT);
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
        return RoleAuthUtils.hasRole(user, roleCode);
    }

    private <T> T safeGet(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int sanitizePageNum(int pageNum) {
        return Math.max(pageNum, 1);
    }

    private int sanitizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime, String message) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, message);
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "最小金额不能小于0");
        }
        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "最大金额不能小于0");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "金额区间不合法，最小金额不能大于最大金额");
        }
    }

    private String rechargeStatusLabel(String status) {
        if ("PENDING".equalsIgnoreCase(status)) {
            return "待审核";
        }
        if ("REVIEW_PENDING".equalsIgnoreCase(status)) {
            return "待复核";
        }
        if ("PASS".equalsIgnoreCase(status)) {
            return "已通过";
        }
        if ("REJECT".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
            return "已驳回";
        }
        return status == null ? "" : status;
    }

    private boolean needDoubleReview(RechargeOrder order) {
        BigDecimal threshold = rechargeDoubleReviewThreshold == null ? BigDecimal.ZERO : rechargeDoubleReviewThreshold;
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return nullSafe(order.getAmount()).compareTo(threshold) >= 0;
    }

    private BigDecimal calculatePercent(long numerator, long denominator) {
        if (denominator <= 0 || numerator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercent(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal safeNumerator = nullSafe(numerator);
        BigDecimal safeDenominator = nullSafe(denominator);
        if (safeNumerator.compareTo(BigDecimal.ZERO) <= 0 || safeDenominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safeNumerator
            .multiply(BigDecimal.valueOf(100))
            .divide(safeDenominator, 2, java.math.RoundingMode.HALF_UP);
    }

    private void fillAnomalyHandleInfo(FinanceAnomalyVO anomaly) {
        if (anomaly == null || anomaly.getOrderId() == null || anomaly.getAnomalyType() == null) {
            return;
        }
        FinanceAnomalyHandle handle = financeAnomalyHandleRepository.findByTypeAndOrderId(
            anomaly.getAnomalyType(),
            anomaly.getOrderId()
        );
        if (handle == null) {
            anomaly.setHandleStatus("PENDING");
            return;
        }
        anomaly.setHandleStatus(handle.getHandleStatus());
        anomaly.setHandleComment(handle.getHandleComment());
        anomaly.setHandlerUserId(handle.getHandlerUserId());
        anomaly.setHandlerUserName(handle.getHandlerUserName());
        anomaly.setHandleTime(handle.getHandleTime());
    }

    private void clearFinanceCache() {
        reconciliationOverviewCache.clear();
    }

    public void notifyFinanceDataChanged() {
        clearFinanceCache();
    }

    private String buildReconciliationOverviewCacheKey(SysUser requester, LocalDateTime startTime, LocalDateTime endTime) {
        Long requesterId = requester == null ? null : requester.getId();
        Long departmentId = requester == null ? null : requester.getDepartmentId();
        return String.join("|",
            "overview",
            normalizeRole(requester),
            Objects.toString(requesterId, "-"),
            Objects.toString(departmentId, "-"),
            Objects.toString(startTime, "-"),
            Objects.toString(endTime, "-")
        );
    }

    private static class CacheEntry {
        private final Map<String, Object> value;
        private final long expireAtMillis;

        private CacheEntry(Map<String, Object> value, long expireAtMillis) {
            this.value = value;
            this.expireAtMillis = expireAtMillis;
        }
    }
}
