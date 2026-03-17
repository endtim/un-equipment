package com.unequipment.platform.modules.system.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.RoleAuthUtils;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysRole;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.entity.SysUserRole;
import com.unequipment.platform.modules.system.repository.SysDepartmentRepository;
import com.unequipment.platform.modules.system.repository.SysRoleRepository;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import com.unequipment.platform.modules.system.repository.SysUserRoleRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SystemAdminService {

    private final SysDepartmentRepository departmentRepository;
    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 角色新增/编辑：
     * - 统一做角色编码标准化与唯一性校验
     * - 角色禁用时清理用户-角色关系，防止禁用角色继续生效
     */
    @Transactional
    public SysRole saveRole(Long id, SysRole request, SysUser operator) {
        assertAdmin(operator);
        if (request == null) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "角色参数不能为空");
        }
        // 角色编码作为系统唯一键：统一标准化后做唯一性校验。
        String roleCode = normalizeCode(request.getRoleCode(), "角色编码不能为空");
        ensureRoleCodeUnique(roleCode, id);

        SysRole role = id == null ? new SysRole() : roleRepository.findById(id);
        if (id != null && role == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "角色不存在");
        }
        role.setRoleName(request.getRoleName());
        role.setRoleCode(roleCode);
        role.setStatus(request.getStatus() == null ? "ENABLED" : request.getStatus());
        role.setRemark(request.getRemark());
        role.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            role.setCreateTime(LocalDateTime.now());
            role.setDeleted(0);
            roleRepository.insert(role);
        } else {
            roleRepository.update(role);
            // 角色禁用时立即解绑用户角色关系，避免禁用角色继续生效。
            if ("DISABLED".equalsIgnoreCase(role.getStatus())) {
                userRoleRepository.deleteByRoleId(role.getId());
            }
        }
        operationLogService.save(operator, "SYSTEM", id == null ? "CREATE_ROLE" : "UPDATE_ROLE", "role:" + role.getRoleCode());
        return role;
    }

    /**
     * 部门新增/编辑：
     * - 部门编码按统一口径标准化并校验唯一性
     * - 更新时保留原有主键与审计信息，仅增量更新可变字段
     */
    @Transactional
    public SysDepartment saveDepartment(Long id, SysDepartment request, SysUser operator) {
        assertAdmin(operator);
        if (request == null) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "部门参数不能为空");
        }
        // 部门编码与角色编码同口径：标准化 + 唯一性校验。
        String deptCode = normalizeCode(request.getDeptCode(), "部门编码不能为空");
        ensureDeptCodeUnique(deptCode, id);

        SysDepartment department = id == null ? new SysDepartment() : departmentRepository.findById(id);
        if (id != null && department == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "部门不存在");
        }
        department.setParentId(request.getParentId());
        department.setDeptName(request.getDeptName());
        department.setDeptCode(deptCode);
        department.setLeaderUserId(request.getLeaderUserId());
        department.setPhone(request.getPhone());
        department.setEmail(request.getEmail());
        department.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        department.setStatus(request.getStatus() == null ? "ENABLED" : request.getStatus());
        department.setRemark(request.getRemark());
        department.setUpdateBy(operator == null ? null : operator.getId());
        department.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            department.setCreateBy(operator == null ? null : operator.getId());
            department.setCreateTime(LocalDateTime.now());
            department.setDeleted(0);
            departmentRepository.insert(department);
        } else {
            departmentRepository.update(department);
        }
        operationLogService.save(
            operator,
            "SYSTEM",
            id == null ? "CREATE_DEPARTMENT" : "UPDATE_DEPARTMENT",
            "department:" + department.getDeptName()
        );
        return department;
    }

    /**
     * 用户新增/编辑：
     * - 先校验“谁可以管谁”（ADMIN 全量；DEPT_MANAGER 仅本部门）
     * - 新增时强制用户名唯一与初始密码
     * - 用户角色采用覆盖式绑定，保证主角色唯一
     */
    @Transactional
    public SysUser saveUser(Long id, SysUser request, SysUser operator) {
        if (request == null) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "用户参数不能为空");
        }
        // 用户管理权限边界：ADMIN 全量，DEPT_MANAGER 仅本部门。
        assertUserManagePermission(id, request, operator);
        SysUser user = id == null ? new SysUser() : userRepository.findById(id);
        if (id != null && user == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "用户不存在");
        }

        if (id == null) {
            if (!StringUtils.hasText(request.getUsername())) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, "用户名不能为空");
            }
            String username = request.getUsername().trim();
            // 用户名全局唯一，创建时必须显式拦截重复。
            ensureUsernameUnique(username, null);
            user.setUsername(username);
            if (!StringUtils.hasText(request.getPassword())) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, "新建用户必须设置密码");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else if (StringUtils.hasText(request.getPassword())) {
            // 编辑时仅在传入新密码时重置，避免误改。
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setRealName(request.getRealName());
        user.setUserType(request.getUserType() == null ? "INTERNAL" : request.getUserType());
        user.setUserNo(request.getUserNo());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setAuthType(request.getAuthType() == null ? "LOCAL" : request.getAuthType());
        user.setDepartmentId(request.getDepartmentId());
        user.setUnitName(request.getUnitName());
        user.setTitleName(request.getTitleName());
        user.setStatus(request.getStatus() == null ? "ENABLED" : request.getStatus());
        user.setRemark(request.getRemark());
        user.setUpdateTime(LocalDateTime.now());
        String roleCode = request.getPrimaryRoleCode() == null ? "INTERNAL_USER" : request.getPrimaryRoleCode().trim();
        if (id == null) {
            user.setCreateTime(LocalDateTime.now());
            user.setDeleted(0);
            userRepository.insert(user);
        } else {
            userRepository.update(user);
        }
        // 用户-角色采用覆盖式绑定，保证主角色唯一。
        bindUserRole(user.getId(), roleCode);
        operationLogService.save(operator, "SYSTEM", id == null ? "CREATE_USER" : "UPDATE_USER", "user:" + user.getUsername());
        return userRepository.findById(user.getId());
    }

    /**
     * 删除部门：
     * - 仅 ADMIN
     * - 部门下仍有用户时禁止删除，避免产生孤儿用户
     */
    @Transactional
    public void deleteDepartment(Long id, SysUser operator) {
        assertAdmin(operator);
        if (departmentRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "部门不存在");
        }
        if (userRepository.countByDepartmentId(id) > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "该部门下仍存在用户，无法删除");
        }
        departmentRepository.softDelete(id, operator == null ? null : operator.getId(), LocalDateTime.now());
        operationLogService.save(operator, "SYSTEM", "DELETE_DEPARTMENT", "departmentId:" + id);
    }

    /**
     * 删除角色：
     * - 禁止删除 ADMIN 角色
     * - 角色仍被用户使用时禁止删除
     */
    @Transactional
    public void deleteRole(Long id, SysUser operator) {
        assertAdmin(operator);
        SysRole role = roleRepository.findById(id);
        if (role == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "角色不存在");
        }
        if ("ADMIN".equalsIgnoreCase(role.getRoleCode())) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "超级管理员角色不可删除");
        }
        if (userRoleRepository.countByRoleId(id) > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "该角色仍被用户使用，无法删除");
        }
        roleRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "SYSTEM", "DELETE_ROLE", "roleId:" + id);
    }

    /**
     * 删除用户：
     * - ADMIN 可删任意用户
     * - DEPT_MANAGER 仅可删本部门用户
     */
    @Transactional
    public void deleteUser(Long id, SysUser operator) {
        SysUser target = userRepository.findById(id);
        if (target == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "用户不存在");
        }
        if (!hasRole(operator, "ADMIN")) {
            if (!hasRole(operator, "DEPT_MANAGER")
                || target.getDepartmentId() == null
                || !target.getDepartmentId().equals(operator.getDepartmentId())) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限删除该用户");
            }
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "SYSTEM", "DELETE_USER", "userId:" + id);
    }

    /**
     * 用户角色覆盖绑定：
     * 先删旧关系再写新关系，确保单用户单主角色映射一致。
     */
    private void bindUserRole(Long userId, String roleCode) {
        SysRole role = roleRepository.findByRoleCode(roleCode);
        if (role == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "角色不存在");
        }
        // 先清理旧绑定，再写入新绑定，避免历史脏映射。
        userRoleRepository.deleteByUserId(userId);
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.insert(userRole);
    }

    /**
     * 用户管理权限边界：
     * - 非 ADMIN 必须是 DEPT_MANAGER
     * - 且目标用户属于本部门
     * - 且不可分配 ADMIN 角色
     */
    private void assertUserManagePermission(Long id, SysUser request, SysUser operator) {
        if (hasRole(operator, "ADMIN")) {
            return;
        }
        // 部门管理员仅可管理本部门，且不得分配 ADMIN 角色。
        if (!hasRole(operator, "DEPT_MANAGER")) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限管理用户");
        }
        Long targetDeptId = request.getDepartmentId();
        if (id != null) {
            SysUser target = userRepository.findById(id);
            if (target == null) {
                throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "用户不存在");
            }
            targetDeptId = target.getDepartmentId();
        }
        if (targetDeptId == null || operator.getDepartmentId() == null || !targetDeptId.equals(operator.getDepartmentId())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "不能管理其他部门用户");
        }
        if ("ADMIN".equalsIgnoreCase(request.getPrimaryRoleCode())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "不能分配超级管理员角色");
        }
    }

    private void assertAdmin(SysUser operator) {
        if (!hasRole(operator, "ADMIN")) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "仅管理员可执行该操作");
        }
    }

    /**
     * 用户名唯一性前置校验，避免数据库异常直接透出。
     */
    private void ensureUsernameUnique(String username, Long excludeId) {
        int count = userRepository.countByUsernameExcludeId(username, excludeId);
        if (count > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "用户名已存在，请更换后重试");
        }
    }

    /**
     * 角色编码唯一性前置校验。
     */
    private void ensureRoleCodeUnique(String roleCode, Long excludeId) {
        int count = roleRepository.countByRoleCodeExcludeId(roleCode, excludeId);
        if (count > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "角色编码已存在，请更换后重试");
        }
    }

    /**
     * 部门编码唯一性前置校验。
     */
    private void ensureDeptCodeUnique(String deptCode, Long excludeId) {
        int count = departmentRepository.countByDeptCodeExcludeId(deptCode, excludeId);
        if (count > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "部门编码已存在，请更换后重试");
        }
    }

    private String normalizeCode(String code, String emptyMessage) {
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, emptyMessage);
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return RoleAuthUtils.hasRole(user, roleCode);
    }
}
