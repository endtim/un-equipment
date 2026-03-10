package com.unequipment.platform.modules.finance.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.finance.dto.RechargeAuditRequest;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class FinanceAdminController {

    private final FinanceService financeService;

    @GetMapping("/recharges")
    public ApiResponse<?> recharges(@CurrentUser SysUser user) {
        return ApiResponse.success(financeService.listRecharges(user));
    }

    @PostMapping("/recharges/{id}/audit")
    public ApiResponse<?> audit(@PathVariable Long id, @Valid @RequestBody RechargeAuditRequest request,
                                @CurrentUser SysUser user) {
        return ApiResponse.success(financeService.auditRecharge(id, request, user));
    }
}
