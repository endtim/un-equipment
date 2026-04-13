package com.unequipment.platform.modules.finance.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.common.util.RoleAuthUtils;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.dto.FinanceAnomalyHandleRequest;
import com.unequipment.platform.modules.finance.dto.FinanceBudgetSaveRequest;
import com.unequipment.platform.modules.finance.dto.FinanceExpenseCreateRequest;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.dto.RechargeRequest;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.entity.FinanceAnomalyHandle;
import com.unequipment.platform.modules.finance.entity.FinanceBudget;
import com.unequipment.platform.modules.finance.entity.FinanceExpense;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import com.unequipment.platform.modules.finance.repository.FinanceAnomalyHandleRepository;
import com.unequipment.platform.modules.finance.repository.FinanceBudgetRepository;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.finance.repository.FinanceDetailRepository;
import com.unequipment.platform.modules.finance.repository.FinanceExpenseRepository;
import com.unequipment.platform.modules.finance.repository.RechargeOrderRepository;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.finance.repository.TransactionRecordRepository;
import com.unequipment.platform.modules.finance.vo.FinanceAnomalyVO;
import com.unequipment.platform.modules.finance.vo.FinanceBudgetWarningVO;
import com.unequipment.platform.modules.finance.vo.FinanceDetailVO;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.repository.InstrumentRepository;
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
    private static final int DETAIL_EXPORT_MAX_ROWS = 20000;
    private static final long RECONCILIATION_OVERVIEW_CACHE_MILLIS = Duration.ofSeconds(45).toMillis();

    private final AccountRepository accountRepository;
    private final RechargeOrderRepository rechargeOrderRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final FinanceExpenseRepository financeExpenseRepository;
    private final FinanceDetailRepository financeDetailRepository;
    private final FinanceBudgetRepository financeBudgetRepository;
    private final InstrumentRepository instrumentRepository;
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
        // 大额充值开启双审后，审批流会从 PENDING -> REVIEW_PENDING -> PASS，避免单人一次性放款。
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
                // 初审阶段仅允许把待审核单推进到“待复核”，通过条件更新防止重复点击导致越级通过。
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
                // 复核阶段再次使用“当前状态匹配”更新，确保并发下只会有一个复核结果生效。
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
        // 结算时只释放“可释放且不超过本次应扣”的冻结金额，避免把历史冻结额度误释放。
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
                // 并发下若结算记录已不处于 PENDING，不允许无条件回写为 CONFIRMED。
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "结算状态已变化，请刷新后重试");
            } else {
                // 历史脏数据可能缺失结算记录，这里兜底补建一条已结算记录，保证订单与财务账一致。
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
        if (settlement == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "结算记录不存在，无法执行退款");
        }
        String settleStatus = normalizeSettlementStatus(settlement.getSettleStatus());
        if ("REFUNDED".equals(settleStatus)) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "订单已退款，请勿重复操作");
        }
        if ("REFUNDING".equals(settleStatus)) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "订单退款处理中，请稍后再试");
        }
        if (!"CONFIRMED".equals(settleStatus) && !"REFUND_PENDING".equals(settleStatus)) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "当前结算状态不允许退款");
        }
        executeRefund(order, settlement.getId(), settleStatus, reason, operator);
    }

    @Transactional
    public void refundForSettlement(ReservationOrder order, Long settlementId, String reason, SysUser operator) {
        executeRefund(order, settlementId, "REFUNDING", reason, operator);
    }

    private void executeRefund(ReservationOrder order, Long settlementId, String currentStatus,
                               String reason, SysUser operator) {
        if (settlementId == null) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "结算记录ID不能为空");
        }
        String expectedStatus = normalizeSettlementStatus(currentStatus);
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
        int updated = settlementRecordRepository.updateStatusByIdWhenCurrent(
            settlementId,
            expectedStatus,
            "REFUNDED",
            operator == null ? null : operator.getId(),
            LocalDateTime.now()
        );
        if (updated <= 0) {
            // 退款状态更新必须命中期望状态，防止重复回调或并发审批把同一结算单重复退款。
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "结算状态已变化，请刷新后重试");
        }
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
            // 下单后先落一条待结算记录，保证订单在结算前也有可追踪的财务占位数据。
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
        // 已存在待结算记录时只刷新金额口径，不改终态字段，避免覆盖人工处理过的结算结果。
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
        // 冻结预估金额前必须校验可用余额，避免“余额充足但可用不足”的并发透支。
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
        // 解冻作为补偿动作不抛余额不足异常，允许在重复回滚场景下保持幂等收敛。
        accountRepository.unfreezeAmount(account.getId(), amount, LocalDateTime.now());
    }

    @Transactional
    public void markSettlementVoid(ReservationOrder order, SysUser operator) {
        SettlementRecord settlement = settlementRecordRepository.findByOrderId(order.getId());
        if (settlement == null) {
            return;
        }
        String currentStatus = normalizeSettlementStatus(settlement.getSettleStatus());
        if ("VOID".equals(currentStatus)) {
            return;
        }
        if (!"PENDING".equals(currentStatus)) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "当前结算状态不允许作废");
        }
        // 仅允许从 PENDING 作废，防止已结算/已退款记录被误作废破坏账务链路。
        int changed = settlementRecordRepository.updateStatusByIdWhenCurrent(
            settlement.getId(),
            "PENDING",
            "VOID",
            operator == null ? null : operator.getId(),
            null
        );
        if (changed <= 0) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "结算状态已变化，请刷新后重试");
        }
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
        // 分页查询统一带上角色范围，确保查询结果与后台权限模型保持一致。
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
        // 导出复用分页查询逻辑，避免导出口径与列表口径出现不一致。
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

    public PageResponse<FinanceDetailVO> pageFinanceDetails(SysUser requester, String keyword, String bizType,
                                                            String inoutType, Long instrumentId, Long departmentId,
                                                            LocalDateTime startTime, LocalDateTime endTime,
                                                            int pageNum, int pageSize) {
        validateTimeRange(startTime, endTime, "经费明细时间范围不合法，开始时间不能晚于结束时间");
        int safePageNum = sanitizePageNum(pageNum);
        int safePageSize = sanitizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        // 经费明细受角色与部门范围双重约束，避免跨部门流水泄露。
        String roleCode = normalizeRole(requester);
        Long scopeDepartmentId = requester == null ? null : requester.getDepartmentId();
        List<FinanceDetailVO> list = financeDetailRepository.findPage(
            trimToNull(keyword),
            trimToNull(bizType),
            trimToNull(inoutType),
            instrumentId,
            departmentId,
            startTime,
            endTime,
            roleCode,
            scopeDepartmentId,
            offset,
            safePageSize
        );
        long total = financeDetailRepository.countPage(
            trimToNull(keyword),
            trimToNull(bizType),
            trimToNull(inoutType),
            instrumentId,
            departmentId,
            startTime,
            endTime,
            roleCode,
            scopeDepartmentId
        );
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    public String exportFinanceDetailsCsv(SysUser requester, String keyword, String bizType, String inoutType,
                                          Long instrumentId, Long departmentId,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        // 明细导出也走统一查询入口，保证筛选条件、权限和汇总口径完全一致。
        List<FinanceDetailVO> list = pageFinanceDetails(
            requester,
            keyword,
            bizType,
            inoutType,
            instrumentId,
            departmentId,
            startTime,
            endTime,
            1,
            DETAIL_EXPORT_MAX_ROWS
        ).getList();
        StringBuilder sb = new StringBuilder();
        sb.append("业务类型,业务单号,订单号,仪器名称,部门,用户,收支方向,金额,发生时间,备注\n");
        for (FinanceDetailVO item : list) {
            sb.append(csv(item.getBizTypeLabel())).append(",")
                .append(csv(item.getBizNo())).append(",")
                .append(csv(item.getOrderNo())).append(",")
                .append(csv(item.getInstrumentName())).append(",")
                .append(csv(item.getDepartmentName())).append(",")
                .append(csv(item.getUserName())).append(",")
                .append(csv(item.getInoutTypeLabel())).append(",")
                .append(csv(item.getAmount())).append(",")
                .append(csv(item.getOccurTime())).append(",")
                .append(csv(item.getRemark())).append("\n");
        }
        return sb.toString();
    }

    @Transactional
    public FinanceExpense createFinanceExpense(SysUser requester, FinanceExpenseCreateRequest request) {
        if (requester == null || (!hasRole(requester, "ADMIN") && !hasRole(requester, "DEPT_MANAGER"))) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权登记经费支出");
        }
        Instrument instrument = instrumentRepository.findById(request.getInstrumentId());
        if (instrument == null || instrument.getDeleted() != null && instrument.getDeleted() == 1) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "仪器不存在");
        }
        if (hasRole(requester, "DEPT_MANAGER")) {
            if (requester.getDepartmentId() == null
                || !requester.getDepartmentId().equals(instrument.getDepartmentId())) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权登记该仪器的维护支出");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        FinanceExpense expense = new FinanceExpense();
        expense.setExpenseNo(BizNoGenerator.next("EXP"));
        expense.setInstrumentId(instrument.getId());
        expense.setDepartmentId(instrument.getDepartmentId());
        expense.setExpenseType(normalizeExpenseType(request.getExpenseType()));
        expense.setAmount(request.getAmount());
        expense.setTitle(request.getTitle().trim());
        expense.setRemark(trimToNull(request.getRemark()));
        expense.setExpenseTime(request.getExpenseTime());
        expense.setOperatorUserId(requester.getId());
        expense.setCreateTime(now);
        expense.setUpdateTime(now);
        financeExpenseRepository.insert(expense);
        operationLogService.save(
            requester,
            "FINANCE",
            "CREATE_FINANCE_EXPENSE",
            "expenseId:" + expense.getId() + ",expenseNo:" + expense.getExpenseNo()
                + ",instrumentId:" + expense.getInstrumentId() + ",amount:" + expense.getAmount()
        );
        clearFinanceCache();
        return expense;
    }

    public PageResponse<FinanceBudget> pageBudgets(SysUser requester, Integer budgetYear,
                                                   Long departmentId, Long instrumentId, String status,
                                                   int pageNum, int pageSize) {
        int safePageNum = sanitizePageNum(pageNum);
        int safePageSize = sanitizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        String roleCode = normalizeRole(requester);
        Long scopeDepartmentId = requester == null ? null : requester.getDepartmentId();
        List<FinanceBudget> list = financeBudgetRepository.findPageByScope(
            budgetYear,
            departmentId,
            instrumentId,
            trimToNull(status),
            roleCode,
            scopeDepartmentId,
            offset,
            safePageSize
        );
        long total = financeBudgetRepository.countPageByScope(
            budgetYear,
            departmentId,
            instrumentId,
            trimToNull(status),
            roleCode,
            scopeDepartmentId
        );
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    @Transactional
    public FinanceBudget saveBudget(SysUser requester, FinanceBudgetSaveRequest request) {
        if (requester == null || (!hasRole(requester, "ADMIN") && !hasRole(requester, "DEPT_MANAGER"))) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权维护预算");
        }
        if (request.getInstrumentId() != null) {
            Instrument instrument = instrumentRepository.findById(request.getInstrumentId());
            if (instrument == null || instrument.getDeleted() != null && instrument.getDeleted() == 1) {
                throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "仪器不存在");
            }
            if (request.getDepartmentId() == null) {
                request.setDepartmentId(instrument.getDepartmentId());
            }
        }
        if (hasRole(requester, "DEPT_MANAGER")) {
            if (request.getDepartmentId() == null
                || requester.getDepartmentId() == null
                || !requester.getDepartmentId().equals(request.getDepartmentId())) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "仅可维护本部门预算");
            }
        }
        if (request.getWarningRatio().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "预警阈值不能大于100");
        }
        FinanceBudget exists = financeBudgetRepository.findByScope(
            request.getBudgetYear(),
            request.getDepartmentId(),
            request.getInstrumentId()
        );
        if (request.getId() == null && exists != null) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "同年度同维度预算已存在");
        }
        if (request.getId() != null && exists != null && !request.getId().equals(exists.getId())) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "同年度同维度预算已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        FinanceBudget budget = request.getId() == null ? new FinanceBudget() : financeBudgetRepository.findById(request.getId());
        if (request.getId() != null && budget == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "预算记录不存在");
        }
        if (request.getId() == null) {
            budget.setBudgetNo(BizNoGenerator.next("BDG"));
            budget.setCreateTime(now);
        }
        budget.setBudgetYear(request.getBudgetYear());
        budget.setDepartmentId(request.getDepartmentId());
        budget.setInstrumentId(request.getInstrumentId());
        budget.setBudgetAmount(request.getBudgetAmount());
        budget.setWarningRatio(request.getWarningRatio());
        budget.setRemark(trimToNull(request.getRemark()));
        budget.setStatus("ENABLED");
        budget.setOperatorUserId(requester.getId());
        budget.setUpdateTime(now);
        if (request.getId() == null) {
            financeBudgetRepository.insert(budget);
        } else {
            financeBudgetRepository.update(budget);
        }
        operationLogService.save(
            requester,
            "FINANCE",
            "SAVE_FINANCE_BUDGET",
            "budgetId:" + budget.getId() + ",budgetNo:" + budget.getBudgetNo()
                + ",year:" + budget.getBudgetYear() + ",departmentId:" + budget.getDepartmentId()
                + ",instrumentId:" + budget.getInstrumentId() + ",amount:" + budget.getBudgetAmount()
        );
        return financeBudgetRepository.findById(budget.getId());
    }

    public List<FinanceBudgetWarningVO> budgetWarnings(SysUser requester, Integer budgetYear) {
        String roleCode = normalizeRole(requester);
        Long scopeDepartmentId = requester == null ? null : requester.getDepartmentId();
        List<FinanceBudget> budgets = financeBudgetRepository.findAllForWarning(budgetYear, roleCode, scopeDepartmentId);
        List<FinanceBudgetWarningVO> result = new java.util.ArrayList<>();
        for (FinanceBudget budget : budgets) {
            BigDecimal usedAmount = nullSafe(financeExpenseRepository.sumAmountByBudgetScope(
                budget.getBudgetYear(),
                budget.getDepartmentId(),
                budget.getInstrumentId(),
                roleCode,
                scopeDepartmentId
            ));
            BigDecimal usedRatio = BigDecimal.ZERO;
            if (budget.getBudgetAmount() != null && budget.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
                usedRatio = usedAmount.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getBudgetAmount(), 2, java.math.RoundingMode.HALF_UP);
            }
            FinanceBudgetWarningVO vo = new FinanceBudgetWarningVO();
            vo.setBudgetId(budget.getId());
            vo.setBudgetNo(budget.getBudgetNo());
            vo.setBudgetYear(budget.getBudgetYear());
            vo.setDepartmentId(budget.getDepartmentId());
            vo.setDepartmentName(budget.getDepartmentName());
            vo.setInstrumentId(budget.getInstrumentId());
            vo.setInstrumentName(budget.getInstrumentName());
            vo.setBudgetAmount(nullSafe(budget.getBudgetAmount()));
            vo.setWarningRatio(nullSafe(budget.getWarningRatio()));
            vo.setUsedAmount(usedAmount);
            vo.setUsedRatio(usedRatio);
            vo.setWarningLevel(resolveWarningLevel(usedRatio, vo.getWarningRatio()));
            result.add(vo);
        }
        return result;
    }

    public Map<String, Object> reconciliationOverview(SysUser requester, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime, "对账时间范围不合法，开始时间不能晚于结束时间");
        try {
            String cacheKey = buildReconciliationOverviewCacheKey(requester, startTime, endTime);
            CacheEntry cached = reconciliationOverviewCache.get(cacheKey);
            long nowMillis = System.currentTimeMillis();
            if (cached != null && cached.expireAtMillis > nowMillis && cached.value != null) {
                return cached.value;
            }
            // 指标聚合失败不应阻断页面，后续 safeGet 会按字段降级为安全默认值。
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
                () -> settlementRecordRepository.countSettledByScopeAndSettledTime(startTime, endTime, roleCode, scopeDepartmentId),
                0L
            );
            BigDecimal settledAmount = nullSafe(safeGet(
                () -> settlementRecordRepository.sumFinalAmountByStatusAndScopeAndSettledTime("CONFIRMED", startTime, endTime, roleCode, scopeDepartmentId),
                BigDecimal.ZERO
            ));
            BigDecimal refundedAmount = nullSafe(safeGet(
                () -> settlementRecordRepository.sumFinalAmountByStatusAndScopeAndSettledTime("REFUNDED", startTime, endTime, roleCode, scopeDepartmentId),
                BigDecimal.ZERO
            ));
            BigDecimal maintenanceExpenseAmount = nullSafe(safeGet(
                () -> financeExpenseRepository.sumAmountByScope(startTime, endTime, roleCode, scopeDepartmentId),
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
                () -> settlementRecordRepository.avgSettleHoursByScopeAndSettledTime(startTime, endTime, roleCode, scopeDepartmentId),
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
            result.put("maintenanceExpenseAmount", maintenanceExpenseAmount);
            result.put("netIncomeAmount", settledAmount.subtract(refundedAmount).subtract(maintenanceExpenseAmount));
            result.put("avgSettleHours", avgSettleHours);
            result.put("avgWaitingSettlementHours", avgWaitingSettlementHours);
            result.put("completedButUnsettled", completedButUnsettled);
            result.put("waitingSettlementOrders", waitingSettlement);
            result.put("confirmedButUnpaidOrders", confirmedButUnpaid);
            result.put("rangeStart", startTime);
            result.put("rangeEnd", endTime);
            // 对账总览短时缓存，减少高频刷新导致的统计 SQL 压力。
            reconciliationOverviewCache.put(cacheKey, new CacheEntry(result, nowMillis + RECONCILIATION_OVERVIEW_CACHE_MILLIS));
            return result;
        } catch (Exception ignored) {
            // 兜底返回全零结构，保证前端看板可渲染且不会因为单点统计异常白屏。
            return buildReconciliationOverviewFallback(startTime, endTime);
        }
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
        String anomalyType = normalizeAnomalyType(request.getAnomalyType());
        Long orderId = request.getOrderId();
        // 处理异常账前先锁订单，避免处理过程与正常结算/退款并发写同一订单状态。
        ReservationOrder order = orderRepository.findByIdForUpdate(orderId);
        if (order == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "订单不存在");
        }
        // 院系管理员只能处理本部门订单，避免越权处理异常账。
        if (hasRole(requester, "DEPT_MANAGER")
            && (requester.getDepartmentId() == null
            || order.getDepartmentId() == null
            || !requester.getDepartmentId().equals(order.getDepartmentId()))) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权处理该订单的异常账");
        }

        if (request.getSettlementId() != null) {
            SettlementRecord settlement = settlementRecordRepository.findById(request.getSettlementId());
            if (settlement == null || !Objects.equals(settlement.getOrderId(), orderId)) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, "结算单与订单不匹配");
            }
        }

        String handleStatus = request.getHandleStatus().trim().toUpperCase(Locale.ROOT);
        if ("RESOLVED".equals(handleStatus)) {
            // “标记已处理”前先执行最小自动修复，并二次校验异常是否真的消失，避免只改状态不修数据。
            autoRepairAnomaly(anomalyType, order, requester);
            ReservationOrder latestOrder = orderRepository.findById(orderId);
            SettlementRecord latestSettlement = settlementRecordRepository.findByOrderId(orderId);
            if (isAnomalyStillExists(anomalyType, latestOrder, latestSettlement)) {
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "异常仍未消除，请先完成对应业务修复后再标记已处理");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        financeAnomalyHandleRepository.upsert(
            anomalyType,
            orderId,
            request.getSettlementId(),
            handleStatus,
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
            "type:" + anomalyType + ",orderId:" + orderId + ",status:" + handleStatus
        );
    }

    private String normalizeAnomalyType(String anomalyType) {
        String type = trimToNull(anomalyType);
        if (type == null) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "异常类型不能为空");
        }
        String upper = type.toUpperCase(Locale.ROOT);
        if (!"COMPLETED_UNSETTLED".equals(upper)
            && !"WAITING_SETTLEMENT".equals(upper)
            && !"CONFIRMED_UNPAID".equals(upper)
            && !"REFUNDED_UNMATCHED".equals(upper)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "异常类型不支持自动处理");
        }
        return upper;
    }

    /**
     * 异常账“已处理”动作会尝试执行最小自动修复：
     * 1) 已完成未结算/待结算滞留：补扣费并落完成态
     * 2) 已结算未支付：补回写支付状态
     * 3) 已退款未冲正：补回写结算退款状态
     */
    private void autoRepairAnomaly(String anomalyType, ReservationOrder order, SysUser operator) {
        SettlementRecord settlement = settlementRecordRepository.findByOrderId(order.getId());
        if ("COMPLETED_UNSETTLED".equals(anomalyType) || "WAITING_SETTLEMENT".equals(anomalyType)) {
            // 完成/待结算异常优先补扣费，再统一回写订单结算与支付状态，确保账务与订单同向收敛。
            if (settlement == null || "PENDING".equalsIgnoreCase(settlement.getSettleStatus())) {
                deductForOrder(order, operator);
            }
            order.setSettlementStatus("CONFIRMED");
            order.setPayStatus("PAID");
            order.setOrderStatus("COMPLETED");
            if (order.getFinishTime() == null) {
                order.setFinishTime(LocalDateTime.now());
            }
            order.setUpdateTime(LocalDateTime.now());
            if (orderRepository.update(order) <= 0) {
                throw new BizException(ErrorCodes.BIZ_ERROR, "订单状态回写失败，请稍后重试");
            }
            return;
        }

        if ("CONFIRMED_UNPAID".equals(anomalyType)) {
            if (settlement == null || !"CONFIRMED".equalsIgnoreCase(settlement.getSettleStatus())) {
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "当前结算状态不允许标记已支付");
            }
            order.setPayStatus("PAID");
            if ("WAITING_SETTLEMENT".equalsIgnoreCase(order.getOrderStatus())
                || "SETTLING".equalsIgnoreCase(order.getOrderStatus())) {
                order.setOrderStatus("COMPLETED");
            }
            order.setSettlementStatus("CONFIRMED");
            order.setUpdateTime(LocalDateTime.now());
            if (orderRepository.update(order) <= 0) {
                throw new BizException(ErrorCodes.BIZ_ERROR, "订单支付状态回写失败，请稍后重试");
            }
            return;
        }

        if ("REFUNDED_UNMATCHED".equals(anomalyType)) {
            if (!"REFUNDED".equalsIgnoreCase(order.getPayStatus())) {
                throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "订单支付状态不是已退款，无法执行冲正修复");
            }
            if (settlement == null) {
                throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "缺少结算记录，无法自动修复，请人工处理");
            }
            String currentStatus = normalizeSettlementStatus(settlement.getSettleStatus());
            if (!"REFUNDED".equals(currentStatus)) {
                settlementRecordRepository.updateStatusById(
                    settlement.getId(),
                    "REFUNDED",
                    operator == null ? null : operator.getId(),
                    LocalDateTime.now()
                );
            }
        }
    }

    private boolean isAnomalyStillExists(String anomalyType, ReservationOrder order, SettlementRecord settlement) {
        if (order == null) {
            return false;
        }
        String orderStatus = trimToNull(order.getOrderStatus());
        String payStatus = trimToNull(order.getPayStatus());
        String settleStatus = settlement == null ? null : trimToNull(settlement.getSettleStatus());
        if ("COMPLETED_UNSETTLED".equals(anomalyType)) {
            return "COMPLETED".equalsIgnoreCase(orderStatus)
                && (settlement == null || "PENDING".equalsIgnoreCase(settleStatus));
        }
        if ("WAITING_SETTLEMENT".equals(anomalyType)) {
            return "WAITING_SETTLEMENT".equalsIgnoreCase(orderStatus);
        }
        if ("CONFIRMED_UNPAID".equals(anomalyType)) {
            return settlement != null
                && "CONFIRMED".equalsIgnoreCase(settleStatus)
                && !"PAID".equalsIgnoreCase(payStatus);
        }
        if ("REFUNDED_UNMATCHED".equals(anomalyType)) {
            return "REFUNDED".equalsIgnoreCase(payStatus)
                && (settlement == null || !"REFUNDED".equalsIgnoreCase(settleStatus));
        }
        return false;
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

    private String normalizeSettlementStatus(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
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
            // 统计类查询统一吞异常并降级，避免某一条 SQL 失败影响整页指标可用性。
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

    private String resolveWarningLevel(BigDecimal usedRatio, BigDecimal warningRatio) {
        BigDecimal ratio = nullSafe(usedRatio);
        BigDecimal threshold = nullSafe(warningRatio);
        // 没有配置阈值时按 80% 的平台默认阈值预警，避免阈值缺失导致预算超用无告警。
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            threshold = BigDecimal.valueOf(80);
        }
        if (ratio.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "OVER_BUDGET";
        }
        if (ratio.compareTo(threshold) >= 0) {
            return "WARNING";
        }
        return "NORMAL";
    }

    private String normalizeExpenseType(String expenseType) {
        String type = trimToNull(expenseType);
        if (type == null) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "支出类型不能为空");
        }
        String upper = type.toUpperCase(Locale.ROOT);
        if (!"MAINTENANCE".equals(upper)
            && !"REPAIR".equals(upper)
            && !"CALIBRATION".equals(upper)
            && !"OTHER".equals(upper)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "支出类型不合法");
        }
        return upper;
    }

    private boolean needDoubleReview(RechargeOrder order) {
        BigDecimal threshold = rechargeDoubleReviewThreshold == null ? BigDecimal.ZERO : rechargeDoubleReviewThreshold;
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        // 金额达到阈值时触发双审，等于阈值也纳入复核，避免边界值绕过风控。
        return nullSafe(order.getAmount()).compareTo(threshold) >= 0;
    }

    private BigDecimal calculatePercent(long numerator, long denominator) {
        if (denominator <= 0 || numerator <= 0) {
            return BigDecimal.ZERO;
        }
        // 统一保留两位小数，和前端看板百分比展示精度保持一致。
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
        // 异常账清单与处理记录分表存储，这里做补齐，保证列表可直接显示处理状态。
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
        // 缓存键包含角色、用户、部门和时间范围，防止不同权限范围复用到错误的汇总结果。
        return String.join("|",
            "overview",
            normalizeRole(requester),
            Objects.toString(requesterId, "-"),
            Objects.toString(departmentId, "-"),
            Objects.toString(startTime, "-"),
            Objects.toString(endTime, "-")
        );
    }

    private Map<String, Object> buildReconciliationOverviewFallback(LocalDateTime startTime, LocalDateTime endTime) {
        // fallback 结构与正常返回字段保持一致，避免前端因字段缺失出现兼容问题。
        Map<String, Object> result = new HashMap<>();
        result.put("rechargeCount", 0L);
        result.put("rechargePassCount", 0L);
        result.put("rechargeAmount", BigDecimal.ZERO);
        result.put("settlementCount", 0L);
        result.put("settledAmount", BigDecimal.ZERO);
        result.put("refundedAmount", BigDecimal.ZERO);
        result.put("rechargePassRate", BigDecimal.ZERO);
        result.put("refundRate", BigDecimal.ZERO);
        result.put("maintenanceExpenseAmount", BigDecimal.ZERO);
        result.put("netIncomeAmount", BigDecimal.ZERO);
        result.put("avgSettleHours", BigDecimal.ZERO);
        result.put("avgWaitingSettlementHours", BigDecimal.ZERO);
        result.put("completedButUnsettled", 0L);
        result.put("waitingSettlementOrders", 0L);
        result.put("confirmedButUnpaidOrders", 0L);
        result.put("rangeStart", startTime);
        result.put("rangeEnd", endTime);
        return result;
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
