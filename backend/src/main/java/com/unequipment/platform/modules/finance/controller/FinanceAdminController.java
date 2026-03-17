package com.unequipment.platform.modules.finance.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.finance.dto.FinanceAnomalyHandleRequest;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import com.unequipment.platform.modules.finance.vo.FinanceAnomalyVO;
import com.unequipment.platform.modules.finance.service.FinanceService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class FinanceAdminController {

    private final FinanceService financeService;

    /**
     * 充值审核单分页查询。
     * 角色范围与过滤条件由后端统一处理，前端不再做权限筛选。
     */
    @GetMapping("/recharges/page")
    public ApiResponse<PageResponse<RechargeOrder>> rechargePage(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long auditUserId,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) BigDecimal maxAmount,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.pageRecharges(
            user, keyword, status, userId, auditUserId, minAmount, maxAmount, startTime, endTime, pageNum, pageSize
        ));
    }

    /**
     * 充值审核单导出（CSV）。
     * 导出口径与分页查询保持一致，避免“看得到导不出/导出范围不一致”。
     */
    @GetMapping(value = "/recharges/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportRechargeCsv(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long auditUserId,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) BigDecimal maxAmount,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        String csv = financeService.exportRechargesCsv(
            user, keyword, status, userId, auditUserId, minAmount, maxAmount, startTime, endTime
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recharges.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv);
    }

    /**
     * 对账总览：返回充值、结算、退款与异常类汇总指标。
     */
    @GetMapping("/reconciliation/overview")
    public ApiResponse<Map<String, Object>> reconciliationOverview(
        @CurrentUser SysUser user,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(financeService.reconciliationOverview(user, startTime, endTime));
    }

    /**
     * 对账异常分页：按异常类型与时间窗口查询。
     */
    @GetMapping("/reconciliation/anomalies")
    public ApiResponse<PageResponse<FinanceAnomalyVO>> reconciliationAnomalies(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(financeService.reconciliationAnomalies(
            user, type, startTime, endTime, pageNum, pageSize
        ));
    }

    @PostMapping("/recharges/{id}/audit")
    public ApiResponse<?> audit(@PathVariable Long id, @Valid @RequestBody RechargeAuditRequest request,
                                @CurrentUser SysUser user) {
        return ApiResponse.success(financeService.auditRecharge(id, request, user));
    }

    @PostMapping("/reconciliation/anomalies/handle")
    public ApiResponse<?> handleAnomaly(@CurrentUser SysUser user,
                                        @Valid @RequestBody FinanceAnomalyHandleRequest request) {
        financeService.handleReconciliationAnomaly(user, request);
        return ApiResponse.success(null);
    }
}
