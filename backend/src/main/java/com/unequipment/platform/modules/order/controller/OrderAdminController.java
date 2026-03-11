package com.unequipment.platform.modules.order.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.order.dto.AuditRequest;
import com.unequipment.platform.modules.order.dto.OrderActionRequest;
import com.unequipment.platform.modules.order.service.OrderService;
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
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderSummaryVO>> allOrders(@CurrentUser SysUser user) {
        return ApiResponse.success(orderService.allOrders(user));
    }

    @PostMapping("/{id}/audit")
    public ApiResponse<?> audit(@PathVariable Long id, @CurrentUser SysUser user, @Valid @RequestBody AuditRequest request) {
        return ApiResponse.success(orderService.audit(id, user, request));
    }

    @PostMapping("/{id}/check-in")
    public ApiResponse<?> checkIn(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.checkIn(id, user));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<?> receiveSample(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.receiveSample(id, user));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<?> finish(@PathVariable Long id, @CurrentUser SysUser user, @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.success(orderService.finishMachine(id, user, request));
    }

    @PostMapping("/{id}/settle")
    public ApiResponse<?> settle(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.settle(id, user));
    }

    @PostMapping("/{id}/result")
    public ApiResponse<?> uploadResult(@PathVariable Long id, @CurrentUser SysUser user,
                                       @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.success(orderService.uploadSampleResult(id, user, request));
    }
}
