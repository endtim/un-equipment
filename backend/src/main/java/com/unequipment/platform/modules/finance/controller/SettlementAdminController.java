package com.unequipment.platform.modules.finance.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.finance.dto.SettlementRefundRequest;
import com.unequipment.platform.modules.finance.service.SettlementAdminService;
import com.unequipment.platform.modules.finance.vo.SettlementAdminVO;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import java.time.LocalDateTime;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
/**
 * 结算管理控制器：
 * 提供结算分页、详情、结算执行、退款申请与退款执行入口。
 */
public class SettlementAdminController {

    private final SettlementAdminService settlementAdminService;

    /**
     * 结算单分页查询。
     */
    @GetMapping
    public ApiResponse<PageResponse<SettlementAdminVO>> page(@CurrentUser SysUser user,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) Long departmentId,
                                                             @RequestParam(required = false) Long orderId,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createStart,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createEnd,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime settledStart,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime settledEnd,
                                                             @RequestParam(defaultValue = "1") int pageNum,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(settlementAdminService.page(user, keyword, status, departmentId, orderId,
            createStart, createEnd, settledStart, settledEnd, pageNum, pageSize));
    }

    /**
     * 结算单详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<SettlementAdminVO> detail(@CurrentUser SysUser user, @PathVariable Long id) {
        return ApiResponse.success(settlementAdminService.detail(user, id));
    }

    /**
     * 执行结算。
     */
    @PostMapping("/{id}/settle")
    public ApiResponse<ReservationOrder> settle(@CurrentUser SysUser user, @PathVariable Long id) {
        return ApiResponse.success(settlementAdminService.settle(user, id));
    }

    /**
     * 执行退款。
     */
    @PostMapping("/{id}/refund")
    public ApiResponse<ReservationOrder> refund(@CurrentUser SysUser user, @PathVariable Long id,
                                                @Valid @RequestBody(required = false) SettlementRefundRequest request) {
        return ApiResponse.success(settlementAdminService.refund(user, id, request));
    }

    /**
     * 发起退款申请（不直接动账）。
     */
    @PostMapping("/{id}/refund-request")
    public ApiResponse<ReservationOrder> requestRefund(@CurrentUser SysUser user, @PathVariable Long id,
                                                       @Valid @RequestBody(required = false) SettlementRefundRequest request) {
        return ApiResponse.success(settlementAdminService.requestRefund(user, id, request));
    }
}
