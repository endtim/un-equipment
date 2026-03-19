package com.unequipment.platform.modules.system.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.system.dto.UserAuditRequest;
import com.unequipment.platform.modules.system.dto.UserStatusUpdateRequest;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysRole;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.service.SystemAdminService;
import javax.validation.Valid;
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

    /**
     * 新增部门。
     */
    @PostMapping("/departments")
    public ApiResponse<SysDepartment> createDepartment(@RequestBody SysDepartment department, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveDepartment(null, department, user));
    }

    /**
     * 编辑部门。
     */
    @PutMapping("/departments/{id}")
    public ApiResponse<SysDepartment> updateDepartment(@PathVariable Long id, @RequestBody SysDepartment department, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveDepartment(id, department, user));
    }

    /**
     * 新增角色。
     */
    @PostMapping("/roles")
    public ApiResponse<SysRole> createRole(@RequestBody SysRole role, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveRole(null, role, user));
    }

    /**
     * 编辑角色。
     */
    @PutMapping("/roles/{id}")
    public ApiResponse<SysRole> updateRole(@PathVariable Long id, @RequestBody SysRole role, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveRole(id, role, user));
    }

    /**
     * 删除部门（软删除）。
     */
    @DeleteMapping("/departments/{id}")
    public ApiResponse<?> deleteDepartment(@PathVariable Long id, @CurrentUser SysUser user) {
        systemAdminService.deleteDepartment(id, user);
        return ApiResponse.success("成功");
    }

    /**
     * 删除角色（软删除）。
     */
    @DeleteMapping("/roles/{id}")
    public ApiResponse<?> deleteRole(@PathVariable Long id, @CurrentUser SysUser user) {
        systemAdminService.deleteRole(id, user);
        return ApiResponse.success("成功");
    }

    /**
     * 新增用户。
     */
    @PostMapping("/users")
    public ApiResponse<SysUser> createUser(@RequestBody SysUser target, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveUser(null, target, user));
    }

    /**
     * 编辑用户。
     */
    @PutMapping("/users/{id}")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUser target, @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.saveUser(id, target, user));
    }

    /**
     * 审核校外注册用户（待审核 -> 通过/驳回）。
     */
    @PostMapping("/users/{id}/audit")
    public ApiResponse<SysUser> auditUser(@PathVariable Long id,
                                          @Valid @RequestBody UserAuditRequest request,
                                          @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.auditExternalUser(id, request, user));
    }

    /**
     * 账号状态调整（启用/禁用等）。
     */
    @PostMapping("/users/{id}/status")
    public ApiResponse<SysUser> updateUserStatus(@PathVariable Long id,
                                                 @Valid @RequestBody UserStatusUpdateRequest request,
                                                 @CurrentUser SysUser user) {
        return ApiResponse.success(systemAdminService.updateUserStatus(id, request, user));
    }

    /**
     * 删除用户（软删除）。
     */
    @DeleteMapping("/users/{id}")
    public ApiResponse<?> deleteUser(@PathVariable Long id, @CurrentUser SysUser user) {
        systemAdminService.deleteUser(id, user);
        return ApiResponse.success("成功");
    }
}
