package com.unequipment.platform.modules.system.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysRole;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysDepartmentRepository;
import com.unequipment.platform.modules.system.repository.SysRoleRepository;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
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
        @RequestParam(defaultValue = "10") int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        return ApiResponse.success(new PageResponse<>(
            userRepository.findPage(keyword, offset, pageSize),
            userRepository.countPage(keyword),
            pageNum,
            pageSize
        ));
    }

    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> roles() {
        return ApiResponse.success(roleRepository.findAll());
    }

    @GetMapping("/departments")
    public ApiResponse<List<SysDepartment>> departments() {
        return ApiResponse.success(departmentRepository.findAll());
    }
}
