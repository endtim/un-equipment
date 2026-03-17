package com.unequipment.platform.modules.order.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.order.dto.MachineReservationRequest;
import com.unequipment.platform.modules.order.dto.SampleReservationRequest;
import com.unequipment.platform.modules.order.service.OrderService;
import com.unequipment.platform.modules.order.vo.OrderDetailVO;
import com.unequipment.platform.modules.order.vo.OrderSummaryVO;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final OrderService orderService;

    /**
     * 前台上机预约创建。
     */
    @PostMapping("/machine")
    public ApiResponse<?> createMachine(@CurrentUser SysUser user, @Valid @RequestBody MachineReservationRequest request) {
        return ApiResponse.success(orderService.createMachineOrder(user, request));
    }

    /**
     * 前台送样预约创建。
     */
    @PostMapping("/sample")
    public ApiResponse<?> createSample(@CurrentUser SysUser user, @Valid @RequestBody SampleReservationRequest request) {
        return ApiResponse.success(orderService.createSampleOrder(user, request));
    }

    /**
     * 我的订单分页（支持按订单类型过滤）。
     */
    @GetMapping("/my")
    public ApiResponse<PageResponse<OrderSummaryVO>> myOrders(@CurrentUser SysUser user,
                                                              @RequestParam(value = "orderType", required = false) String orderType,
                                                              @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                                              @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return ApiResponse.success(orderService.myOrders(user, orderType, pageNum, pageSize));
    }

    /**
     * 订单详情（仅本人或有管理权限角色可见）。
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.detail(id, user));
    }

    /**
     * 前台用户主动取消订单。
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.cancel(id, user));
    }

}
