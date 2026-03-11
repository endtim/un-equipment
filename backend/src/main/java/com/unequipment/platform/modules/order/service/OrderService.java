package com.unequipment.platform.modules.order.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.service.InstrumentService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.assembler.OrderAssembler;
import com.unequipment.platform.modules.order.dto.AuditRequest;
import com.unequipment.platform.modules.order.dto.MachineReservationRequest;
import com.unequipment.platform.modules.order.dto.OrderActionRequest;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
        statusMap.put(ACTION_CANCEL, setOf("PENDING_AUDIT", "APPROVED", "WAITING_USE", "WAITING_RECEIVE"));
        ACTION_ALLOWED_STATUS = Collections.unmodifiableMap(statusMap);

        Map<String, Set<String>> typeMap = new HashMap<>();
        typeMap.put(ACTION_AUDIT, setOf(TYPE_MACHINE, TYPE_SAMPLE));
        typeMap.put(ACTION_CHECK_IN, setOf(TYPE_MACHINE));
        typeMap.put(ACTION_RECEIVE_SAMPLE, setOf(TYPE_SAMPLE));
        typeMap.put(ACTION_UPLOAD_RESULT, setOf(TYPE_SAMPLE));
        typeMap.put(ACTION_FINISH_USE, setOf(TYPE_MACHINE));
        typeMap.put(ACTION_SETTLE, setOf(TYPE_MACHINE, TYPE_SAMPLE));
        typeMap.put(ACTION_CANCEL, setOf(TYPE_MACHINE, TYPE_SAMPLE));
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

    @Transactional
    public ReservationOrder createMachineOrder(SysUser user, MachineReservationRequest request) {
        Instrument instrument = instrumentService.getById(request.getInstrumentId());
        instrumentService.ensureReservable(instrument);
        instrumentService.ensureOrderTypeSupported(instrument, TYPE_MACHINE, user);
        instrumentService.validateMachineReserveWindow(instrument, request.getReservedStart(), request.getReservedEnd());
        if (orderRepository.countMachineConflict(instrument.getId(), request.getReservedEnd(), request.getReservedStart()) > 0) {
            throw new BizException(ErrorCodes.ORDER_TIME_CONFLICT, "reservation time conflicts with existing order");
        }

        BigDecimal hours = BigDecimal.valueOf(Duration.between(request.getReservedStart(), request.getReservedEnd()).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal amount = nullSafe(instrument.getPriceInternal()).multiply(hours);
        financeService.ensureEnoughBalance(user, amount);

        ReservationOrder order = buildBaseOrder(user, instrument, TYPE_MACHINE);
        order.setReserveStart(request.getReservedStart());
        order.setReserveEnd(request.getReservedEnd());
        order.setReserveMinutes((int) Duration.between(request.getReservedStart(), request.getReservedEnd()).toMinutes());
        order.setEstimatedAmount(amount);
        order.setFinalAmount(amount);
        order.setRemark(request.getRemark());
        orderRepository.insert(order);
        appendAudit(order, user, "PENDING", "SUBMIT", "Machine order submitted");
        messageService.send(user, "Reservation submitted", "Machine reservation submitted and waiting for audit.");
        operationLogService.save(user, "ORDER", "CREATE_MACHINE_ORDER", "orderId:" + order.getId());
        return getOrder(order.getId());
    }

    @Transactional
    public ReservationOrder createSampleOrder(SysUser user, SampleReservationRequest request) {
        Instrument instrument = instrumentService.getById(request.getInstrumentId());
        instrumentService.ensureReservable(instrument);
        instrumentService.ensureOrderTypeSupported(instrument, TYPE_SAMPLE, user);
        BigDecimal amount = nullSafe(instrument.getPriceExternal()).multiply(BigDecimal.valueOf(request.getSampleCount()));
        financeService.ensureEnoughBalance(user, amount);

        ReservationOrder order = buildBaseOrder(user, instrument, TYPE_SAMPLE);
        order.setReserveMinutes(0);
        order.setEstimatedAmount(amount);
        order.setFinalAmount(amount);
        order.setRemark(request.getRemark());
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

        appendAudit(order, user, "PENDING", "SUBMIT", "Sample order submitted");
        messageService.send(user, "Sample reservation submitted", "Sample reservation submitted and waiting for audit.");
        operationLogService.save(user, "ORDER", "CREATE_SAMPLE_ORDER", "orderId:" + order.getId());
        return getOrder(order.getId());
    }

    @Transactional
    public ReservationOrder audit(Long orderId, SysUser auditor, AuditRequest request) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, auditor);
        assertActionAllowed(order, ACTION_AUDIT);
        if (!"APPROVE".equalsIgnoreCase(request.getAction()) && !"REJECT".equalsIgnoreCase(request.getAction())) {
            throw new BizException(ErrorCodes.ORDER_INVALID_ACTION, "invalid audit action");
        }

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            order.setAuditStatus("PASS");
            order.setOrderStatus(TYPE_MACHINE.equals(order.getOrderType()) ? "WAITING_USE" : "WAITING_RECEIVE");
            order.setApproveTime(LocalDateTime.now());
            messageService.send(simpleUser(order.getUserId()), "Order approved", "Your order was approved.");
        } else {
            order.setAuditStatus("REJECT");
            order.setOrderStatus("REJECTED");
            messageService.send(simpleUser(order.getUserId()), "Order rejected", "Your order was rejected.");
        }
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, auditor, order.getAuditStatus(), request.getAction().toUpperCase(), request.getComment());
        operationLogService.save(auditor, "ORDER", "AUDIT_ORDER", "orderId:" + orderId + ":" + request.getAction());
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder checkIn(Long orderId, SysUser operator) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        assertActionAllowed(order, ACTION_CHECK_IN);
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

        appendAudit(order, operator, "PASS", "CHECK_IN", "Machine usage started");
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
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "sample detail missing");
        }
        sampleOrder.setReceiveStatus("RECEIVED");
        sampleOrder.setTestingStatus("TESTING");
        sampleOrder.setReceivedTime(LocalDateTime.now());
        sampleOrder.setReceiverUserId(operator.getId());
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.update(sampleOrder);
        appendAudit(order, operator, "PASS", "RECEIVE_SAMPLE", "Sample received");
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
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "sample detail missing");
        }
        sampleOrder.setResultSummary(request.getComment());
        sampleOrder.setTestingStatus("RESULT_UPLOADED");
        sampleOrder.setUpdateTime(LocalDateTime.now());
        sampleOrderRepository.update(sampleOrder);
        order.setOrderStatus("WAITING_SETTLEMENT");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, operator, "PASS", "UPLOAD_RESULT", "Sample result uploaded");
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
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "usage record missing");
        }
        usage.setEndTime(LocalDateTime.now());
        if (usage.getStartTime() != null) {
            usage.setActualMinutes((int) Duration.between(usage.getStartTime(), usage.getEndTime()).toMinutes());
        }
        usage.setAbnormalDesc(request.getComment());
        usage.setUpdateTime(LocalDateTime.now());
        usageRecordRepository.update(usage);

        order.setOrderStatus("WAITING_SETTLEMENT");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, operator, "PASS", "FINISH_USE", "Machine usage finished");
        operationLogService.save(operator, "ORDER", "FINISH_MACHINE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder settle(Long orderId, SysUser operator) {
        ReservationOrder order = getOrder(orderId);
        assertManageable(order, operator);
        assertActionAllowed(order, ACTION_SETTLE);
        financeService.deductForOrder(order);
        order.setSettlementStatus("CONFIRMED");
        order.setPayStatus("PAID");
        order.setOrderStatus("COMPLETED");
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, operator, "PASS", "SETTLE", "Order settled");
        messageService.send(simpleUser(order.getUserId()), "Order completed", "Your order has been settled.");
        operationLogService.save(operator, "ORDER", "SETTLE_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    @Transactional
    public ReservationOrder cancel(Long orderId, SysUser user) {
        ReservationOrder order = getOrder(orderId);
        if (!order.getUserId().equals(user.getId())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "cannot cancel others order");
        }
        assertActionAllowed(order, ACTION_CANCEL);
        if ("PAID".equalsIgnoreCase(order.getPayStatus())
            && !"REFUNDED".equalsIgnoreCase(order.getPayStatus())) {
            financeService.refundForOrder(order, "Order canceled by user");
            order.setPayStatus("REFUNDED");
            order.setSettlementStatus("REFUNDED");
        } else {
            order.setSettlementStatus("VOID");
        }
        order.setOrderStatus("CANCELED");
        order.setCancelReason("Canceled by user");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.update(order);
        appendAudit(order, user, "PASS", "CANCEL", "Order canceled by user");
        messageService.send(user, "Order canceled", "Your order has been canceled.");
        operationLogService.save(user, "ORDER", "CANCEL_ORDER", "orderId:" + orderId);
        return getOrder(orderId);
    }

    public List<OrderSummaryVO> myOrders(SysUser user) {
        return orderAssembler.toSummaryList(orderRepository.findByUserId(user.getId()));
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
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "order not found");
        }
        return order;
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
        auditRecord.setAuditorId(operator.getId());
        auditRecord.setAuditorRole(operator.getPrimaryRoleCode());
        auditRecord.setAuditResult(result);
        auditRecord.setAuditOpinion(action + (opinion == null ? "" : ": " + opinion));
        auditRecord.setAuditTime(LocalDateTime.now());
        auditRecord.setCreateTime(LocalDateTime.now());
        auditRecordRepository.insert(auditRecord);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private SysUser simpleUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        return user;
    }

    private void assertManageable(ReservationOrder order, SysUser user) {
        if (!canManage(order, user)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied for this order");
        }
    }

    private void assertVisible(ReservationOrder order, SysUser user) {
        if (!canView(order, user)) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "cannot access this order");
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
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }

    private void assertActionAllowed(ReservationOrder order, String action) {
        Set<String> allowedTypes = ACTION_ALLOWED_TYPE.getOrDefault(action, Collections.emptySet());
        if (!allowedTypes.contains(order.getOrderType())) {
            throw new BizException(ErrorCodes.ORDER_TYPE_NOT_ALLOWED, "action is not allowed for this order type");
        }
        Set<String> allowedStatuses = ACTION_ALLOWED_STATUS.getOrDefault(action, Collections.emptySet());
        if (!allowedStatuses.contains(order.getOrderStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, "order status does not allow this action");
        }
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }
}
