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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
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
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(new PageResponse<>(
                userRepository.findPage(keyword, offset, pageSize),
                userRepository.countPage(keyword),
                pageNum,
                pageSize
            ));
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            if (currentUser.getDepartmentId() == null) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "department manager has no department bound");
            }
            return ApiResponse.success(new PageResponse<>(
                userRepository.findPageByDepartment(currentUser.getDepartmentId(), keyword, offset, pageSize),
                userRepository.countPageByDepartment(currentUser.getDepartmentId(), keyword),
                pageNum,
                pageSize
            ));
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied");
    }

    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> roles(@CurrentUser SysUser currentUser) {
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(roleRepository.findAll());
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            return ApiResponse.success(roleRepository.findAllNonAdmin());
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied");
    }

    @GetMapping("/departments")
    public ApiResponse<List<SysDepartment>> departments(@CurrentUser SysUser currentUser) {
        if (hasRole(currentUser, "ADMIN")) {
            return ApiResponse.success(departmentRepository.findAll());
        }
        if (hasRole(currentUser, "DEPT_MANAGER")) {
            SysDepartment currentDepartment = currentUser.getDepartmentId() == null
                ? null
                : departmentRepository.findById(currentUser.getDepartmentId());
            if (currentDepartment == null) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "department manager has no department bound");
            }
            return ApiResponse.success(java.util.Collections.singletonList(currentDepartment));
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied");
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }
}
