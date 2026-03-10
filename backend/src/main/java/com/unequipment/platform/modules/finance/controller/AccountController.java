package com.unequipment.platform.modules.finance.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.finance.dto.RechargeRequest;
import com.unequipment.platform.modules.finance.service.FinanceService;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final FinanceService financeService;

    @GetMapping("/account/me")
    public ApiResponse<?> me(@CurrentUser SysUser user) {
        return ApiResponse.success(financeService.accountInfo(user));
    }

    @PostMapping("/recharges")
    public ApiResponse<?> submitRecharge(@CurrentUser SysUser user, @Valid @RequestBody RechargeRequest request) {
        return ApiResponse.success(financeService.submitRecharge(user, request));
    }
}
