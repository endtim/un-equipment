package com.unequipment.platform.modules.order.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.order.dto.AuditRequest;
import com.unequipment.platform.modules.order.dto.OrderAmountAdjustRequest;
import com.unequipment.platform.modules.order.dto.OrderActionRequest;
import com.unequipment.platform.modules.order.service.OrderService;
import com.unequipment.platform.modules.order.vo.OrderSummaryVO;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    /**
     * 管理端订单分页查询入口。
     * 仅负责接收查询条件并转交 Service，状态流转与权限判定统一在 Service 层完成。
     */
    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryVO>> allOrders(@CurrentUser SysUser user,
                                                               @RequestParam(required = false) String orderType,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submitStart,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submitEnd,
                                                               @RequestParam(required = false) Long departmentId,
                                                               @RequestParam(required = false) String auditorKeyword,
                                                               @RequestParam(required = false) BigDecimal minAmount,
                                                               @RequestParam(required = false) BigDecimal maxAmount,
                                                               @RequestParam(defaultValue = "1") int pageNum,
                                                               @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(orderService.pageOrders(user, orderType, status, keyword, submitStart, submitEnd,
            departmentId, auditorKeyword, minAmount, maxAmount, pageNum, pageSize));
    }

    /**
     * 订单审核动作入口（通过/拒绝）。
     */
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

    /**
     * 管理端执行结算入口。
     */
    @PostMapping("/{id}/settle")
    public ApiResponse<?> settle(@PathVariable Long id, @CurrentUser SysUser user) {
        return ApiResponse.success(orderService.settle(id, user));
    }

    /**
     * 管理端关闭订单（非支付订单终止流程）。
     */
    @PostMapping("/{id}/close")
    public ApiResponse<?> close(@PathVariable Long id, @CurrentUser SysUser user,
                                @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.success(orderService.close(id, user, request));
    }

    @PostMapping("/{id}/adjust-amount")
    public ApiResponse<?> adjustAmount(@PathVariable Long id, @CurrentUser SysUser user,
                                       @Valid @RequestBody OrderAmountAdjustRequest request) {
        return ApiResponse.success(orderService.adjustAmount(id, user, request));
    }

    @PostMapping("/{id}/result")
    public ApiResponse<?> uploadResult(@PathVariable Long id, @CurrentUser SysUser user,
                                       @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.success(orderService.uploadSampleResult(id, user, request));
    }
}
