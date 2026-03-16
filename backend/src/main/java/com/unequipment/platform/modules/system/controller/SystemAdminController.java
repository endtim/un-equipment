package com.unequipment.platform.modules.system.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysRole;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysDepartmentRepository;
import com.unequipment.platform.modules.system.repository.SysRoleRepository;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
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

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysDepartmentRepository departmentRepository;

    @GetMapping("/users")
    public ApiResponse<PageResponse<SysUser>> users(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @CurrentUser SysUser currentUser) {
        int validPageNum = Math.max(pageNum, 1);
        int validPageSize = Math.max(pageSize, 1);
        int offset = (validPageNum - 1) * validPageSize;
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(new PageResponse<>(
                userRepository.findPage(keyword, offset, validPageSize),
                userRepository.countPage(keyword),
                validPageNum,
                validPageSize
            ));
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            if (currentUser.getDepartmentId() == null) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "部门管理员未绑定部门");
            }
            return ApiResponse.success(new PageResponse<>(
                userRepository.findPageByDepartment(currentUser.getDepartmentId(), keyword, offset, validPageSize),
                userRepository.countPageByDepartment(currentUser.getDepartmentId(), keyword),
                validPageNum,
                validPageSize
            ));
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限操作");
    }

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

    @GetMapping("/roles/page")
    public ApiResponse<PageResponse<SysRole>> rolePage(@CurrentUser SysUser currentUser,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        int validPageNum = Math.max(pageNum, 1);
        int validPageSize = Math.max(pageSize, 1);
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

    @GetMapping("/departments/page")
    public ApiResponse<PageResponse<SysDepartment>> departmentPage(@CurrentUser SysUser currentUser,
                                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                                   @RequestParam(defaultValue = "10") int pageSize) {
        int validPageNum = Math.max(pageNum, 1);
        int validPageSize = Math.max(pageSize, 1);
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
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }
}
