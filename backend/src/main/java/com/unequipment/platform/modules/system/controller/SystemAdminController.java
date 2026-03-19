package com.unequipment.platform.modules.system.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.RoleAuthUtils;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysRole;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.entity.UserAuditLog;
import com.unequipment.platform.modules.system.repository.SysDepartmentRepository;
import com.unequipment.platform.modules.system.repository.SysRoleRepository;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import com.unequipment.platform.modules.system.service.SystemAdminService;
import com.unequipment.platform.security.CurrentUser;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class SystemAdminController {
    private static final int MAX_PAGE_SIZE = 100;

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysDepartmentRepository departmentRepository;
    private final SystemAdminService systemAdminService;

    /**
     * 用户分页：
     * - ADMIN：全量
     * - DEPT_MANAGER：仅本部门
     */
    @GetMapping("/users")
    public ApiResponse<PageResponse<SysUser>> users(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @CurrentUser SysUser currentUser) {
        int validPageNum = Math.max(pageNum, 1);
        int validPageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int offset = (validPageNum - 1) * validPageSize;
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(new PageResponse<>(
                userRepository.findPage(keyword, status, offset, validPageSize),
                userRepository.countPage(keyword, status),
                validPageNum,
                validPageSize
            ));
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            if (currentUser.getDepartmentId() == null) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "部门管理员未绑定部门");
            }
            return ApiResponse.success(new PageResponse<>(
                userRepository.findPageByDepartment(currentUser.getDepartmentId(), keyword, status, offset, validPageSize),
                userRepository.countPageByDepartment(currentUser.getDepartmentId(), keyword, status),
                validPageNum,
                validPageSize
            ));
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作");
    }

    /**
     * 用户审核记录分页：用于注册审核和状态管控追踪。
     */
    @GetMapping("/users/{id}/audit-logs")
    public ApiResponse<PageResponse<UserAuditLog>> userAuditLogs(@org.springframework.web.bind.annotation.PathVariable Long id,
                                                                 @RequestParam(defaultValue = "1") int pageNum,
                                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                                 @CurrentUser SysUser currentUser) {
        return ApiResponse.success(systemAdminService.pageUserAuditLogs(id, pageNum, pageSize, currentUser));
    }

    /**
     * 角色全量列表（下拉专用）：
     * - ADMIN：全部角色
     * - DEPT_MANAGER：排除 ADMIN
     */
    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> roles(@CurrentUser SysUser currentUser) {
        // Full list endpoint: used by user/permission form dropdowns.
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(roleRepository.findAll());
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            return ApiResponse.success(roleRepository.findAllNonAdmin());
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作");
    }

    /**
     * 角色分页列表，权限口径与 roles 接口一致。
     */
    @GetMapping("/roles/page")
    public ApiResponse<PageResponse<SysRole>> rolePage(@CurrentUser SysUser currentUser,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        int validPageNum = Math.max(pageNum, 1);
        int validPageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int offset = (validPageNum - 1) * validPageSize;
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(new PageResponse<>(
                roleRepository.findPage(offset, validPageSize),
                roleRepository.countPage(),
                validPageNum,
                validPageSize
            ));
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            return ApiResponse.success(new PageResponse<>(
                roleRepository.findPageNonAdmin(offset, validPageSize),
                roleRepository.countPageNonAdmin(),
                validPageNum,
                validPageSize
            ));
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作");
    }

    /**
     * 部门全量列表（下拉专用）：
     * - ADMIN：全部部门
     * - DEPT_MANAGER：仅当前绑定部门
     */
    @GetMapping("/departments")
    public ApiResponse<List<SysDepartment>> departments(@CurrentUser SysUser currentUser) {
        // Full list endpoint: used by user/order filter dropdowns.
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(departmentRepository.findAll());
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            SysDepartment currentDepartment = currentUser.getDepartmentId() == null
                ? null
                : departmentRepository.findById(currentUser.getDepartmentId());
            if (currentDepartment == null) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "部门管理员未绑定部门");
            }
            return ApiResponse.success(Collections.singletonList(currentDepartment));
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作");
    }

    /**
     * 部门分页列表，权限口径与 departments 接口一致。
     */
    @GetMapping("/departments/page")
    public ApiResponse<PageResponse<SysDepartment>> departmentPage(@CurrentUser SysUser currentUser,
                                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                                   @RequestParam(defaultValue = "10") int pageSize) {
        int validPageNum = Math.max(pageNum, 1);
        int validPageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int offset = (validPageNum - 1) * validPageSize;
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(new PageResponse<>(
                departmentRepository.findPage(offset, validPageSize),
                departmentRepository.countPage(),
                validPageNum,
                validPageSize
            ));
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            SysDepartment currentDepartment = currentUser.getDepartmentId() == null
                ? null
                : departmentRepository.findById(currentUser.getDepartmentId());
            if (currentDepartment == null) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "部门管理员未绑定部门");
            }
            return ApiResponse.success(new PageResponse<>(
                Collections.singletonList(currentDepartment), 1, validPageNum, validPageSize
            ));
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作");
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return RoleAuthUtils.hasRole(user, roleCode);
    }
}
