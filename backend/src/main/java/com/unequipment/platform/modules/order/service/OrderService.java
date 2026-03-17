package com.unequipment.platform.modules.order.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.common.util.RoleAuthUtils;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.service.InstrumentService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.assembler.OrderAssembler;
import com.unequipment.platform.modules.order.dto.AuditRequest;
import com.unequipment.platform.modules.order.dto.MachineReservationRequest;
import com.unequipment.platform.modules.order.dto.OrderActionRequest;
import com.unequipment.platform.modules.order.dto.OrderAmountAdjustRequest;
import com.unequipment.platform.modules.order.dto.SampleReservationRequest;
import com.unequipment.platform.modules.order.entity.AuditRecord;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.entity.SampleOrder;
import com.unequipment.platform.modules.order.entity.UsageRecord;
import com.unequipment.platform.modules.order.repository.AuditRecordRepository;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.order.repository.SampleOrderRepository;
import com.unequipment.platform.modules.order.repository.UsageRecordRepository;
import com.unequipment.platform.modules.order.vo.OrderDetailVO;
import com.unequipment.platform.modules.order.vo.OrderSummaryVO;
import com.unequipment.platform.modules.order.vo.ReservedSlotVO;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String TYPE_MACHINE = "MACHINE";
    private static final String TYPE_SAMPLE = "SAMPLE";
    private static final String ACTION_AUDIT = "AUDIT";
    private static final String ACTION_CHECK_IN = "CHECK_IN";
    private static final String ACTION_RECEIVE_SAMPLE = "RECEIVE_SAMPLE";
    private static final String ACTION_UPLOAD_RESULT = "UPLOAD_RESULT";
    private static final String ACTION_FINISH_USE = "FINISH_USE";
    private static final String ACTION_SETTLE = "SETTLE";
    private static final String ACTION_CANCEL = "CANCEL";
    private static final String ACTION_CLOSE = "CLOSE";
    private static final String ACTION_ADJUST_AMOUNT = "ADJUST_AMOUNT";
    private static final int CREATE_RETRY_TIMES = 3;

    private static final Map<String, Set<String>> ACTION_ALLOWED_STATUS;
    private static final Map<String, Set<String>> ACTION_ALLOWED_TYPE;

    static {
        Map<String, Set<String>> statusMap = new HashMap<>();
        statusMap.put(ACTION_AUDIT, setOf("PENDING_AUDIT"));
        statusMap.put(ACTION_CHECK_IN, setOf("WAITING_USE"));
        statusMap.put(ACTION_RECEIVE_SAMPLE, setOf("WAITING_RECEIVE"));
        statusMap.put(ACTION_UPLOAD_RESULT, setOf("TESTING"));
        statusMap.put(ACTION_FINISH_USE, setOf("IN_USE"));
        statusMap.put(ACTION_SETTLE, setOf("WAITING_SETTLEMENT"));
        statusMap.put(ACTION_CANCEL, setOf("PENDING_AUDIT", "WAITING_USE", "WAITING_RECEIVE"));
        statusMap.put(ACTION_CLOSE, setOf("PENDING_AUDIT", "WAITING_USE", "IN_USE", "WAITING_RECEIVE", "TESTING", "WAITING_SETTLEMENT"));
        statusMap.put(ACTION_ADJUST_AMOUNT, setOf("WAITING_SETTLEMENT"));
        ACTION_ALLOWED_STATUS = Collections.unmodifiableMap(statusMap);

        Map<String, Set<String>> typeMap = new HashMap<>();
        typeMap.put(ACTION_AUDIT, setOf(TYPE_MACHINE, TYPE_SAMPLE));
        typeMap.put(ACTION_CHECK_IN, setOf(TYPE_MACHINE));
        typeMap.put(ACTION_RECEIVE_SAMPLE, setOf(TYPE_SAMPLE));
        typeMap.put(ACTION_UPLOAD_RESULT, setOf(TYPE_SAMPLE));
        typeMap.put(ACTION_FINISH_USE, setOf(TYPE_MACHINE));
        typeMap.put(ACTION_SETTLE, setOf(TYPE_MACHINE, TYPE_SAMPLE));
        typeMap.put(ACTION_CANCEL, setOf(TYPE_MACHINE, TYPE_SAMPLE));
        typeMap.put(ACTION_CLOSE, setOf(TYPE_MACHINE, TYPE_SAMPLE));
        typeMap.put(ACTION_ADJUST_AMOUNT, setOf(TYPE_SAMPLE));
        ACTION_ALLOWED_TYPE = Collections.unmodifiableMap(typeMap);
    }

    private final ReservationOrderRepository orderRepository;
    private final SampleOrderRepository sampleOrderRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final AuditRecordRepository auditRecordRepository;
    private final InstrumentService instrumentService;
    private final FinanceService financeService;
    private final MessageService messageService;
    private final OperationLogService operationLogService;
    private final OrderAssembler orderAssembler;
    private final SysUserRepository userRepository;

    @Value("${app.finance.internal-single-limit:0}")
    private BigDecimal internalSingleLimit;
    @Value("${app.finance.external-single-limit:0}")
    private BigDecimal externalSingleLimit;

    @Transactional
    public ReservationOrder createMachineOrder(SysUser user, MachineReservationRequest request) {
        Instrument instrument = instrumentService.getById(request.getInstrumentId());
        instrumentService.ensureReservable(instrument);
        instrumentService.ensureOrderTypeSupported(instrument, TYPE_MACHINE, user);
        instrumentService.validateMachineReserveWindow(instrument, request.getReservedStart(), request.getReservedEnd());

        ReservationOrder created = null;
        BizException lastConflict = null;
        for (int attempt = 1; attempt <= CREATE_RETRY_TIMES; attempt++) {
            try {
                ensureNoMachineConflict(instrument.getId(), request.getReservedStart(), request.getReservedEnd(), null);
                BigDecimal hours = BigDecimal.valueOf(Duration.between(request.getReservedStart(), request.getReservedEnd()).toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                BigDecimal amount = resolveUnitPrice(instrument, user).multiply(hours);
                ensureWithinSingleLimit(user, amount);

                ReservationOrder order = buildBaseOrder(user, instrument, TYPE_MACHINE);
                order.setProjectName(request.getProjectName());
                order.setPurpose(request.getPurpose());
                order.setReserveStart(request.getReservedStart());
                order.setReserveEnd(request.getReservedEnd());
                order.setReserveMinutes((int) Duration.between(request.getReservedStart(), request.getReservedEnd()).toMinutes());
                order.setEstimatedAmount(amount);
                order.setFinalAmount(amount);
                order.setRemark(request.getRemark());
                applyAuditPolicy(order, instrument);

                ensureNoMachineConflict(instrument.getId(), request.getReservedStart(), request.getReservedEnd(), null);
                orderRepository.insert(order);
                if ("PASS".equalsIgnoreCase(order.getAuditStatus())) {
                    financeService.freezeForOrder(order);
                    appendAudit(order, user, "PASS", "AUTO_APPROVE", "上机预约提交后自动通过");
                    messageService.send(user, "预约提交成功", "您的上机预约已提交并自动通过。");
                } else {
                    appendAudit(order, user, "PENDING", "SUBMIT", "上机预约已提交，等待审核");
                    messageService.send(user, "预约提交成功", "您的上机预约已提交，等待审核。");
                }
                operationLogService.save(user, "ORDER", "CREATE_MACHINE_ORDER", "orderId:" + order.getId());
                created = order;
                break;
            } catch (BizException ex) {
                if (ex.getCode() != ErrorCodes.ORDER_TIME_CONFLICT) {
                    throw ex;
                }
                lastConflict = ex;
                if (attempt >= CREATE_RETRY_TIMES) {
                    break;
                }
                shortBackoff(attempt);
            }
        }
        if (created == null) {
            if (lastConflict != null) {
                throw lastConflict;
            }
            throw new BizException(ErrorCodes.BIZ_ERROR, "预约创建失败，请稍后重试");
        }
        return getOrder(created.getId());
    }

    @Transactional
    public ReservationOrder createSampleOrder(SysUser user, SampleReservationRequest request) {
        Instrument instrument = instrumentService.getById(request.getInstrumentId());
        instrumentService.ensureReservable(instrument);
        instrumentService.ensureOrderTypeSupported(instrument, TYPE_SAMPLE, user);
        BigDecimal amount = resolveUnitPrice(instrument, user).multiply(BigDecimal.valueOf(request.getSampleCount()));
        ensureWithinSingleLimit(user, amount);

        ReservationOrder order = buildBaseOrder(user, instrument, TYPE_SAMPLE);
        order.setProjectName(request.getProjectName());
        order.setPurpose(request.getPurpose());
        order.setReserveMinutes(0);
        order.setEstimatedAmount(amount);
        order.setFinalAmount(amount);
        order.setRemark(request.getRemark());
        applyAuditPolicy(order, instrument);
        orderRepository.insert(order);

        SampleOrder sampleOrder = new SampleOrder();
        sampleOrder.setOrderId(order.getId());
        sampleOrder.setSampleName(request.getSampleName());
        sampleOrder.setSampleCount(request.getSampleCount());
        sampleOrder.setReceiveStatus("WAITING_RECEIVE");
        sampleOrder.setTestingStatus("WAITING_RECEIVE");
        sampleOrder.setDangerFlag(0);
        sampleOrder.setCreateTime(LocalDateTime.now());
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.insert(sampleOrder);

        if ("PASS".equalsIgnoreCase(order.getAuditStatus())) {
            financeService.freezeForOrder(order);
            appendAudit(order, user, "PASS", "AUTO_APPROVE", "送样预约提交后自动通过");
            messageService.send(user, "预约提交成功", "您的送样预约已提交并自动通过。");
        } else {
            appendAudit(order, user, "PENDING", "SUBMIT", "送样预约已提交，等待审核");
            messageService.send(user, "预约提交成功", "您的送样预约已提交，等待审核。");
        }
        operationLogService.save(user, "ORDER", "CREATE_SAMPLE_ORDER", "orderId:" + order.getId());
        return getOrder(order.getId());
    }

    @Transactional
    public ReservationOrder audit(Long orderId, SysUser auditor, AuditRequest request) {
        ReservationOrder order = getOrderForUpdate(orderId);
        assertManageable(order, auditor);
        if (auditor != null && order.getUserId() != null && order.getUserId().equals(auditor.getId())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "不能审核自己提交的订单");
        }
        assertActionAllowed(order, ACTION_AUDIT);

        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new BizException(ErrorCodes.ORDER_INVALID_ACTION, "审核动作不合法");
        }

        if ("APPROVE".equals(action)) {
            if (TYPE_MACHINE.equals(order.getOrderType())) {
                ensureNoMachineConflict(order.getInstrumentId(), order.getReserveStart(), order.getReserveEnd(), order.getId());
            }
            financeService.ensureEnoughAvailableBalance(simpleUser(order.getUserId()), nullSafe(order.getEstimatedAmount()));
            financeService.freezeForOrder(order);
            order.setAuditStatus("PASS");
            order.setOrderStatus(TYPE_MACHINE.equals(order.getOrderType()) ? "WAITING_USE" : "WAITING_RECEIVE");
            order.setApproveTime(LocalDateTime.now());
            messageService.send(simpleUser(order.getUserId()), "订单审核通过", "您的订单已审核通过。");
        } else {
            order.setAuditStatus("REJECT");
            order.setOrderStatus("REJECTED");
            messageService.send(simpleUser(order.getUserId()), "订单审核不通过", "您的订单未通过审核。");
        }

        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, auditor, order.getAuditStatus(), action, request.getComment());
        operationLogService.save(auditor, "ORDER", "AUDIT_ORDER", "orderId:" + orderId + ":" + action);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder checkIn(Long orderId, SysUser operator) {
        ReservationOrder order = getOrderForUpdate(orderId);
        assertManageable(order, operator);
        assertActionAllowed(order, ACTION_CHECK_IN);
        assertMachineCheckInTime(order, LocalDateTime.now());

        order.setOrderStatus("IN_USE");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);

        UsageRecord usage = usageRecordRepository.findByOrderId(orderId);
        if (usage == null) {
            usage = new UsageRecord();
            usage.setOrderId(orderId);
            usage.setInstrumentId(order.getInstrumentId());
            usage.setOperatorUserId(order.getUserId());
            usage.setActualMinutes(0);
            usage.setAbnormalFlag(0);
            usage.setCreateTime(LocalDateTime.now());
        }
        usage.setCheckinTime(LocalDateTime.now());
        usage.setStartTime(LocalDateTime.now());
        usage.setUpdateTime(LocalDateTime.now());
        if (usage.getId() == null) {
            usageRecordRepository.insert(usage);
        } else {
            usageRecordRepository.update(usage);
        }

        appendAudit(order, operator, "PASS", "CHECK_IN", "上机签到成功");
        operationLogService.save(operator, "ORDER", "CHECK_IN_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder receiveSample(Long orderId, SysUser operator) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        assertActionAllowed(order, ACTION_RECEIVE_SAMPLE);

        order.setOrderStatus("TESTING");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);

        SampleOrder sampleOrder = sampleOrderRepository.findByOrderId(orderId);
        if (sampleOrder == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "送样明细不存在");
        }
        sampleOrder.setReceiveStatus("RECEIVED");
        sampleOrder.setTestingStatus("TESTING");
        sampleOrder.setReceivedTime(LocalDateTime.now());
        sampleOrder.setReceiverUserId(operator.getId());
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.update(sampleOrder);

        appendAudit(order, operator, "PASS", "RECEIVE_SAMPLE", "送样已接收");
        operationLogService.save(operator, "ORDER", "RECEIVE_SAMPLE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder uploadSampleResult(Long orderId, SysUser operator, OrderActionRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        assertActionAllowed(order, ACTION_UPLOAD_RESULT);

        SampleOrder sampleOrder = sampleOrderRepository.findByOrderId(orderId);
        if (sampleOrder == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "送样明细不存在");
        }
        sampleOrder.setResultSummary(request.getComment());
        sampleOrder.setTestingStatus("RESULT_UPLOADED");
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.update(sampleOrder);

        order.setOrderStatus("WAITING_SETTLEMENT");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        financeService.ensurePendingSettlementRecord(order);
        appendAudit(order, operator, "PASS", "UPLOAD_RESULT", "检测结果已上传");
        operationLogService.save(operator, "ORDER", "UPLOAD_SAMPLE_RESULT", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder finishMachine(Long orderId, SysUser operator, OrderActionRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        assertActionAllowed(order, ACTION_FINISH_USE);

        UsageRecord usage = usageRecordRepository.findByOrderId(orderId);
        if (usage == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "上机记录不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (usage.getStartTime() == null) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "未签到，不能结束上机");
        }
        if (now.isBefore(usage.getStartTime())) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "结束时间不能早于开始时间");
        }

        usage.setEndTime(now);
        long seconds = ChronoUnit.SECONDS.between(usage.getStartTime(), usage.getEndTime());
        int actualMinutes = (int) Math.max(1L, (seconds + 59) / 60);
        usage.setActualMinutes(actualMinutes);
        usage.setAbnormalDesc(request.getComment());
        usage.setUpdateTime(LocalDateTime.now());
        usageRecordRepository.update(usage);

        Instrument instrument = instrumentService.getById(order.getInstrumentId());
        SysUser orderUser = userRepository.findById(order.getUserId());
        int billMinutes = usage.getActualMinutes() == null || usage.getActualMinutes() <= 0 ? 1 : usage.getActualMinutes();
        BigDecimal hours = BigDecimal.valueOf(billMinutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = resolveUnitPrice(instrument, orderUser).multiply(hours);
        order.setFinalAmount(finalAmount);

        order.setOrderStatus("WAITING_SETTLEMENT");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        financeService.ensurePendingSettlementRecord(order);
        appendAudit(order, operator, "PASS", "FINISH_USE", "上机已结束，等待结算");
        operationLogService.save(operator, "ORDER", "FINISH_MACHINE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder settle(Long orderId, SysUser operator) {
        ReservationOrder order = getOrder(orderId);
        assertFinancialManagePermission(operator);
        assertManageable(order, operator);
        assertActionAllowed(order, ACTION_SETTLE);

        int changed = orderRepository.markSettling(orderId, LocalDateTime.now());
        if (changed <= 0) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "订单正在结算或已完成结算，请勿重复操作");
        }
        order.setOrderStatus("SETTLING");
        financeService.deductForOrder(order, operator);
        order.setSettlementStatus("CONFIRMED");
        order.setPayStatus("PAID");
        order.setOrderStatus("COMPLETED");
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, operator, "PASS", "SETTLE", "订单已结算");
        messageService.send(simpleUser(order.getUserId()), "订单已完成", "您的订单已完成结算。");
        operationLogService.save(operator, "ORDER", "SETTLE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder cancel(Long orderId, SysUser user) {
        ReservationOrder order = getOrder(orderId);
        if (!order.getUserId().equals(user.getId())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "不能取消他人的订单");
        }
        String expectedStatus = order.getOrderStatus();
        assertActionAllowed(order, ACTION_CANCEL);
        assertUserCancelable(order);
        if ("PAID".equalsIgnoreCase(order.getPayStatus())) {
            financeService.refundForOrder(order, "用户取消订单", user);
            order.setPayStatus("REFUNDED");
            order.setSettlementStatus("REFUNDED");
        } else {
            financeService.releaseFreezeForOrder(order);
            order.setSettlementStatus("VOID");
            financeService.markSettlementVoid(order, user);
        }

        order.setOrderStatus("CANCELED");
        order.setCancelReason("用户取消");
        order.setUpdateTime(LocalDateTime.now());
        updateOrderWithExpectedStatus(order, expectedStatus, "订单状态已变化，请刷新后重试");
        appendAudit(order, user, "PASS", "CANCEL", "用户取消订单");
        messageService.send(user, "订单已取消", "您的订单已取消。");
        operationLogService.save(user, "ORDER", "CANCEL_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder close(Long orderId, SysUser user, OrderActionRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertFinancialManagePermission(user);
        assertManageable(order, user);
        String expectedStatus = order.getOrderStatus();
        assertActionAllowed(order, ACTION_CLOSE);
        if ("PAID".equalsIgnoreCase(order.getPayStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "已支付订单不能直接关闭，请走退款流程");
        }
        financeService.releaseFreezeForOrder(order);
        order.setOrderStatus("CANCELED");
        order.setSettlementStatus("VOID");
        order.setCancelReason(request == null || request.getComment() == null || request.getComment().trim().isEmpty()
            ? "管理员关闭订单"
            : request.getComment().trim());
        order.setUpdateTime(LocalDateTime.now());
        updateOrderWithExpectedStatus(order, expectedStatus, "订单状态已变化，请刷新后重试");
        financeService.markSettlementVoid(order, user);
        appendAudit(order, user, "PASS", "CLOSE", order.getCancelReason());
        operationLogService.save(user, "ORDER", "CLOSE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder adjustAmount(Long orderId, SysUser user, OrderAmountAdjustRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertFinancialManagePermission(user);
        assertManageable(order, user);
        assertActionAllowed(order, ACTION_ADJUST_AMOUNT);
        order.setFinalAmount(nullSafe(request.getFinalAmount()));
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        financeService.ensurePendingSettlementRecord(order);
        appendAudit(order, user, "PASS", "ADJUST_AMOUNT", request.getComment());
        operationLogService.save(user, "ORDER", "ADJUST_ORDER_AMOUNT", "orderId:" + orderId);
        return getOrder(orderId);
    }

    public PageResponse<OrderSummaryVO> myOrders(SysUser user, String orderType, int pageNum, int pageSize) {
        String normalizedOrderType = orderType == null ? null : orderType.trim().toUpperCase();
        int validPageNum = Math.max(1, pageNum);
        int validPageSize = Math.max(1, pageSize);
        int offset = (validPageNum - 1) * validPageSize;
        List<ReservationOrder> orders = orderRepository.findPageByUser(
            user.getId(), normalizedOrderType, offset, validPageSize
        );
        long total = orderRepository.countByUser(user.getId(), normalizedOrderType);
        return new PageResponse<>(orderAssembler.toSummaryList(orders), total, validPageNum, validPageSize);
    }

    public OrderDetailVO detail(Long orderId, SysUser user) {
        ReservationOrder order = getOrder(orderId);
        assertVisible(order, user);
        UsageRecord usage = usageRecordRepository.findByOrderId(orderId);
        SampleOrder sampleOrder = sampleOrderRepository.findByOrderId(orderId);
        return orderAssembler.toDetail(order, auditRecordRepository.findByOrderId(orderId), usage, sampleOrder);
    }

    public List<OrderSummaryVO> allOrders(SysUser user) {
        return orderAssembler.toSummaryList(orderRepository.findAll().stream()
            .filter(order -> canView(order, user))
            .collect(Collectors.toList()));
    }

    @Transactional
    public int autoCloseExpiredPendingAudit(LocalDateTime cutoffTime, int batchSize) {
        int safeBatch = Math.max(1, Math.min(batchSize, 500));
        List<ReservationOrder> expiredOrders = orderRepository.findPendingAuditExpired(cutoffTime, safeBatch);
        int closed = 0;
        for (ReservationOrder row : expiredOrders) {
            ReservationOrder order = getOrderForUpdate(row.getId());
            if (!"PENDING_AUDIT".equals(order.getOrderStatus())) {
                continue;
            }
            order.setOrderStatus("CANCELED");
            order.setAuditStatus("REJECT");
            order.setSettlementStatus("VOID");
            order.setCancelReason("审核超时自动关闭");
            order.setUpdateTime(LocalDateTime.now());
            updateOrderWithExpectedStatus(order, "PENDING_AUDIT", "订单状态已变化，请刷新后重试");
            appendAudit(order, null, "REJECT", "TIMEOUT_CLOSE", "待审核超时自动关闭");
            messageService.send(simpleUser(order.getUserId()), "订单已自动关闭", "您的订单因审核超时已自动关闭");
            closed++;
        }
        return closed;
    }

    public PageResponse<OrderSummaryVO> pageOrders(SysUser user, String orderType, String status,
                                                   String keyword, LocalDateTime submitStart, LocalDateTime submitEnd,
                                                   Long filterDepartmentId, String auditorKeyword,
                                                   BigDecimal minAmount, BigDecimal maxAmount,
                                                   int pageNum, int pageSize) {
        if (!canManageAsAdmin(user)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限访问");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "最小金额不能大于最大金额");
        }
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        String roleCode = resolveManageRoleCode(user);
        List<ReservationOrder> list = orderRepository.findPageForAdmin(
            orderType, status, keyword, submitStart, submitEnd,
            filterDepartmentId, auditorKeyword, minAmount, maxAmount,
            roleCode, user.getId(), user.getDepartmentId(), offset, pageSize
        );
        long total = orderRepository.countForAdmin(
            orderType, status, keyword, submitStart, submitEnd,
            filterDepartmentId, auditorKeyword, minAmount, maxAmount,
            roleCode, user.getId(), user.getDepartmentId()
        );
        return new PageResponse<>(orderAssembler.toSummaryList(list), total, pageNum, pageSize);
    }

    public List<ReservedSlotVO> listMachineReservedSlots(Long instrumentId, LocalDate date) {
        instrumentService.getById(instrumentId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        return orderRepository.findMachineReservedSlots(instrumentId, dayStart, dayEnd).stream()
            .map(order -> orderAssembler.toReservedSlot(order, dayStart, dayEnd))
            .collect(Collectors.toList());
    }

    public ReservationOrder getOrder(Long orderId) {
        ReservationOrder order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private ReservationOrder getOrderForUpdate(Long orderId) {
        ReservationOrder order = orderRepository.findByIdForUpdate(orderId);
        if (order == null) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void updateOrderWithExpectedStatus(ReservationOrder order, String expectedStatus, String failMessage) {
        int changed = orderRepository.updateByIdAndStatus(order, expectedStatus);
        if (changed <= 0) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, failMessage);
        }
    }

    private ReservationOrder buildBaseOrder(SysUser user, Instrument instrument, String orderType) {
        ReservationOrder order = new ReservationOrder();
        String orderPrefix = TYPE_MACHINE.equals(orderType) ? "ORDM" : "ORDS";
        order.setOrderNo(BizNoGenerator.next(orderPrefix));
        order.setOrderType(orderType);
        order.setUserId(user.getId());
        order.setInstrumentId(instrument.getId());
        order.setDepartmentId(user.getDepartmentId());
        order.setOwnerUserId(instrument.getOwnerUserId());
        order.setContactName(user.getRealName());
        order.setContactPhone(user.getPhone() == null ? "-" : user.getPhone());
        order.setReserveMinutes(0);
        order.setOrderStatus("PENDING_AUDIT");
        order.setAuditStatus("PENDING");
        order.setPayStatus("UNPAID");
        order.setSettlementStatus("PENDING");
        order.setEstimatedAmount(BigDecimal.ZERO);
        order.setFinalAmount(BigDecimal.ZERO);
        order.setSource("WEB");
        order.setSubmitTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }

    private void appendAudit(ReservationOrder order, SysUser operator, String result, String action, String opinion) {
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setOrderId(order.getId());
        Integer maxNode = auditRecordRepository.findMaxNodeNo(order.getId());
        auditRecord.setNodeNo(maxNode == null ? 1 : maxNode + 1);
        Long auditorId = operator == null ? null : operator.getId();
        if (auditorId == null) {
            // 定时任务等系统动作没有登录用户时，使用订单关联用户兜底，避免审计记录主键约束失败
            auditorId = order.getOwnerUserId() != null ? order.getOwnerUserId() : order.getUserId();
        }
        if (auditorId == null) {
            auditorId = 1L;
        }
        auditRecord.setAuditorId(auditorId);
        auditRecord.setAuditorRole(operator == null ? "SYSTEM" : operator.getPrimaryRoleCode());
        auditRecord.setAuditResult(result);
        auditRecord.setAuditOpinion(action + (opinion == null ? "" : ": " + opinion));
        auditRecord.setAuditTime(LocalDateTime.now());
        auditRecord.setCreateTime(LocalDateTime.now());
        auditRecordRepository.insert(auditRecord);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal resolveUnitPrice(Instrument instrument, SysUser user) {
        if (isExternalUser(user)) {
            return nullSafe(instrument.getPriceExternal());
        }
        return nullSafe(instrument.getPriceInternal());
    }

    private boolean isExternalUser(SysUser user) {
        if (user == null) {
            return false;
        }
        if ("EXTERNAL_USER".equalsIgnoreCase(user.getPrimaryRoleCode())) {
            return true;
        }
        return "EXTERNAL".equalsIgnoreCase(user.getUserType());
    }

    private void applyAuditPolicy(ReservationOrder order, Instrument instrument) {
        if (instrument.getNeedAudit() != null && instrument.getNeedAudit() == 0) {
            order.setAuditStatus("PASS");
            order.setOrderStatus(TYPE_MACHINE.equals(order.getOrderType()) ? "WAITING_USE" : "WAITING_RECEIVE");
            order.setApproveTime(LocalDateTime.now());
        } else {
            order.setAuditStatus("PENDING");
            order.setOrderStatus("PENDING_AUDIT");
        }
    }

    private void ensureWithinSingleLimit(SysUser user, BigDecimal amount) {
        BigDecimal limit = isExternalUser(user) ? nullSafe(externalSingleLimit) : nullSafe(internalSingleLimit);
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (nullSafe(amount).compareTo(limit) > 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "单笔订单金额超过预算限额");
        }
    }

    private void ensureNoMachineConflict(Long instrumentId, LocalDateTime reserveStart, LocalDateTime reserveEnd, Long excludeOrderId) {
        int conflict;
        if (excludeOrderId == null) {
            conflict = orderRepository.countMachineConflict(instrumentId, reserveEnd, reserveStart);
        } else {
            conflict = orderRepository.countMachineConflictExcludeOrder(instrumentId, reserveEnd, reserveStart, excludeOrderId);
        }
        if (conflict > 0) {
            throw new BizException(ErrorCodes.ORDER_TIME_CONFLICT, "预约时间与已有订单冲突，请调整时间后重试");
        }
    }

    private void shortBackoff(int attempt) {
        long millis = 30L * attempt;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void assertMachineCheckInTime(ReservationOrder order, LocalDateTime now) {
        if (order.getReserveStart() == null || order.getReserveEnd() == null) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "预约时间缺失，无法签到");
        }
        if (now.isBefore(order.getReserveStart())) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "未到预约开始时间，暂不能签到");
        }
        if (now.isAfter(order.getReserveEnd())) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "预约时段已结束，不能签到");
        }
    }

    private void assertUserCancelable(ReservationOrder order) {
        if (!TYPE_MACHINE.equals(order.getOrderType())) {
            return;
        }
        if (!"WAITING_USE".equals(order.getOrderStatus())) {
            return;
        }
        if (order.getReserveStart() == null) {
            return;
        }
        if (!LocalDateTime.now().plusHours(2).isBefore(order.getReserveStart())) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "开机前2小时内不可自行取消，请联系管理员处理");
        }
    }

    private SysUser simpleUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        return user;
    }

    private void assertManageable(ReservationOrder order, SysUser user) {
        if (!canManage(order, user)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作该订单");
        }
    }

    private void assertVisible(ReservationOrder order, SysUser user) {
        if (!canView(order, user)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限查看该订单");
        }
    }

    private boolean canManage(ReservationOrder order, SysUser user) {
        if (hasRole(user, "ADMIN")) {
            return true;
        }
        if (hasRole(user, "INSTRUMENT_OWNER")) {
            return order.getOwnerUserId() != null && order.getOwnerUserId().equals(user.getId());
        }
        if (hasRole(user, "DEPT_MANAGER")) {
            return order.getDepartmentId() != null && order.getDepartmentId().equals(user.getDepartmentId());
        }
        return false;
    }

    private boolean canView(ReservationOrder order, SysUser user) {
        if (order.getUserId() != null && order.getUserId().equals(user.getId())) {
            return true;
        }
        return canManage(order, user);
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return RoleAuthUtils.hasRole(user, roleCode);
    }

    private boolean canManageAsAdmin(SysUser user) {
        return hasRole(user, "ADMIN") || hasRole(user, "INSTRUMENT_OWNER") || hasRole(user, "DEPT_MANAGER");
    }

    private void assertFinancialManagePermission(SysUser user) {
        if (hasRole(user, "ADMIN") || hasRole(user, "DEPT_MANAGER")) {
            return;
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权执行结算或金额调整操作");
    }

    private String resolveManageRoleCode(SysUser user) {
        if (hasRole(user, "ADMIN")) {
            return "ADMIN";
        }
        if (hasRole(user, "INSTRUMENT_OWNER")) {
            return "INSTRUMENT_OWNER";
        }
        if (hasRole(user, "DEPT_MANAGER")) {
            return "DEPT_MANAGER";
        }
        return defaultString(user == null ? null : user.getPrimaryRoleCode(), "INTERNAL_USER").toUpperCase();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void assertActionAllowed(ReservationOrder order, String action) {
        Set<String> allowedTypes = ACTION_ALLOWED_TYPE.getOrDefault(action, Collections.emptySet());
        if (!allowedTypes.contains(order.getOrderType())) {
            throw new BizException(ErrorCodes.ORDER_TYPE_NOT_ALLOWED, "当前订单类型不支持该操作");
        }
        Set<String> allowedStatuses = ACTION_ALLOWED_STATUS.getOrDefault(action, Collections.emptySet());
        if (!allowedStatuses.contains(order.getOrderStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "当前订单状态不允许执行该操作");
        }
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }
}
