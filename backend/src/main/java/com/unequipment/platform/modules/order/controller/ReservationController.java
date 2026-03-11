package com.unequipment.platform.modules.order.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.order.dto.MachineReservationRequest;
import com.unequipment.platform.modules.order.dto.OrderActionRequest;
import com.unequipment.platform.modules.order.dto.SampleReservationRequest;
import com.unequipment.platform.modules.order.service.OrderService;
import com.unequipment.platform.modules.order.vo.OrderDetailVO;
import com.unequipment.platform.modules.order.vo.OrderSummaryVO;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final OrderService orderService;

    @PostMapping("/machine")
    public ApiResponse<?> createMachine(@CurrentUser SysUser user, @Valid @RequestBody MachineReservationRequest request) {
        return ApiResponse.success(orderService.createMachineOrder(user, request));
    }

    @PostMapping("/sample")
    public ApiResponse<?> createSample(@CurrentUser SysUser user, @Valid @RequestBody SampleReservationRequest request) {
        return ApiResponse.success(orderService.createSampleOrder(user, request));
    }

    @GetMapping("/my")
    public ApiResponse<List<OrderSummaryVO>> myOrders(@CurrentUser SysUser user) {
        return ApiResponse.success(orderService.myOrders(user));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.detail(id, user));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.cancel(id, user));
    }

    @PostMapping("/{id}/result")
    public ApiResponse<?> uploadResult(@PathVariable Long id, @CurrentUser SysUser user,
                                       @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.success(orderService.uploadSampleResult(id, user, request));
    }
}
