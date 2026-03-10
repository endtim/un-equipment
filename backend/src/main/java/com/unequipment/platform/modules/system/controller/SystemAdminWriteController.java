package com.unequipment.platform.modules.system.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysRole;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.service.SystemAdminService;
import com.unequipment.platform.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class SystemAdminWriteController {

    private final SystemAdminService systemAdminService;

    @PostMapping("/departments")
    public ApiResponse<SysDepartment> createDepartment(@RequestBody SysDepartment department, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveDepartment(null, department, user));
    }

    @PutMapping("/departments/{id}")
    public ApiResponse<SysDepartment> updateDepartment(@PathVariable Long id, @RequestBody SysDepartment department, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveDepartment(id, department, user));
    }

    @PostMapping("/roles")
    public ApiResponse<SysRole> createRole(@RequestBody SysRole role, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveRole(null, role, user));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<SysRole> updateRole(@PathVariable Long id, @RequestBody SysRole role, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveRole(id, role, user));
    }

    @DeleteMapping("/departments/{id}")
    public ApiResponse<?> deleteDepartment(@PathVariable Long id, @CurrentUser SysUser user) {
        systemAdminService.deleteDepartment(id, user);
        return ApiResponse.success("ok");
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<?> deleteRole(@PathVariable Long id, @CurrentUser SysUser user) {
        systemAdminService.deleteRole(id, user);
        return ApiResponse.success("ok");
    }

    @PostMapping("/users")
    public ApiResponse<SysUser> createUser(@RequestBody SysUser target, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveUser(null, target, user));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUser target, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveUser(id, target, user));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<?> deleteUser(@PathVariable Long id, @CurrentUser SysUser user) {
        systemAdminService.deleteUser(id, user);
        return ApiResponse.success("ok");
    }
}
