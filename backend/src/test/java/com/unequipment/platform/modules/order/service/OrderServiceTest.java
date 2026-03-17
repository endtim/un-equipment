package com.unequipment.platform.modules.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.instrument.service.InstrumentService;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.assembler.OrderAssembler;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.AuditRecordRepository;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.order.repository.SampleOrderRepository;
import com.unequipment.platform.modules.order.repository.UsageRecordRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ReservationOrderRepository orderRepository;
    @Mock
    private SampleOrderRepository sampleOrderRepository;
    @Mock
    private UsageRecordRepository usageRecordRepository;
    @Mock
    private AuditRecordRepository auditRecordRepository;
    @Mock
    private InstrumentService instrumentService;
    @Mock
    private FinanceService financeService;
    @Mock
    private MessageService messageService;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private OrderAssembler orderAssembler;
    @Mock
    private SysUserRepository userRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderRepository,
            sampleOrderRepository,
            usageRecordRepository,
            auditRecordRepository,
            instrumentService,
            financeService,
            messageService,
            operationLogService,
            orderAssembler,
            userRepository
        );
    }

    @Test
    void cancel_shouldRejectWhenMachineOrderWithinTwoHoursBeforeStart() {
        SysUser user = new SysUser();
        user.setId(100L);

        ReservationOrder order = new ReservationOrder();
        order.setId(1L);
        order.setUserId(100L);
        order.setOrderType("MACHINE");
        order.setOrderStatus("WAITING_USE");
        order.setPayStatus("UNPAID");
        order.setReserveStart(LocalDateTime.now().plusMinutes(30));
        order.setReserveEnd(LocalDateTime.now().plusHours(2));

        when(orderRepository.findById(1L)).thenReturn(order);

        BizException ex = assertThrows(BizException.class, () -> orderService.cancel(1L, user));
        assertEquals(ErrorCodes.ORDER_STATUS_NOT_ALLOWED, ex.getCode());

        verify(financeService, never()).refundForOrder(any(), any(), any());
        verify(financeService, never()).releaseFreezeForOrder(any());
        verify(orderRepository, never()).updateByIdAndStatus(any(), any());
    }

    @Test
    void autoCloseExpiredPendingAudit_shouldCloseEligibleOrders() {
        ReservationOrder expiredRow = new ReservationOrder();
        expiredRow.setId(11L);
        when(orderRepository.findPendingAuditExpired(any(), eq(200)))
            .thenReturn(Collections.singletonList(expiredRow));

        ReservationOrder lockedOrder = new ReservationOrder();
        lockedOrder.setId(11L);
        lockedOrder.setUserId(200L);
        lockedOrder.setOrderStatus("PENDING_AUDIT");
        when(orderRepository.findByIdForUpdate(11L)).thenReturn(lockedOrder);
        when(orderRepository.updateByIdAndStatus(any(ReservationOrder.class), eq("PENDING_AUDIT"))).thenReturn(1);
        when(auditRecordRepository.findMaxNodeNo(11L)).thenReturn(null);

        int closed = orderService.autoCloseExpiredPendingAudit(LocalDateTime.now(), 200);
        assertEquals(1, closed);

        verify(orderRepository).updateByIdAndStatus(any(ReservationOrder.class), eq("PENDING_AUDIT"));
        verify(auditRecordRepository).insert(any());
        verify(messageService).send(any(), any(), any());
    }
}

