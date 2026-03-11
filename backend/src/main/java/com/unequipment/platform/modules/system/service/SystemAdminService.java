package com.unequipment.platform.modules.system.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
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

    @Transactional
    public SysRole saveRole(Long id, SysRole request, SysUser operator) {
        assertAdmin(operator);
        SysRole role = id == null ? new SysRole() : roleRepository.findById(id);
        if (id != null && role == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "role not found");
        }
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setStatus(request.getStatus() == null ? "ENABLED" : request.getStatus());
        role.setRemark(request.getRemark());
        role.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            role.setCreateTime(LocalDateTime.now());
            role.setDeleted(0);
            roleRepository.insert(role);
        } else {
            roleRepository.update(role);
        }
        operationLogService.save(operator, "SYSTEM", id == null ? "CREATE_ROLE" : "UPDATE_ROLE", "role:" + role.getRoleCode());
        return role;
    }

    @Transactional
    public SysDepartment saveDepartment(Long id, SysDepartment request, SysUser operator) {
        assertAdmin(operator);
        SysDepartment department = id == null ? new SysDepartment() : departmentRepository.findById(id);
        if (department == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "department not found");
        }
        department.setParentId(request.getParentId());
        department.setDeptName(request.getDeptName());
        department.setDeptCode(request.getDeptCode());
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
        operationLogService.save(operator, "SYSTEM", id == null ? "CREATE_DEPARTMENT" : "UPDATE_DEPARTMENT", "department:" + department.getDeptName());
        return department;
    }

    @Transactional
    public SysUser saveUser(Long id, SysUser request, SysUser operator) {
        assertUserManagePermission(id, request, operator);
        SysUser user = id == null ? new SysUser() : userRepository.findById(id);
        if (id != null && user == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "user not found");
        }
        user.setUsername(id == null ? request.getUsername() : user.getUsername());
        if (id == null) {
            if (!StringUtils.hasText(request.getPassword())) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, "password is required when creating user");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else if (StringUtils.hasText(request.getPassword())) {
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
        String roleCode = request.getPrimaryRoleCode() == null ? "INTERNAL_USER" : request.getPrimaryRoleCode();
        if (id == null) {
            user.setCreateTime(LocalDateTime.now());
            user.setDeleted(0);
            userRepository.insert(user);
        } else {
            userRepository.update(user);
        }
        bindUserRole(user.getId(), roleCode);
        operationLogService.save(operator, "SYSTEM", id == null ? "CREATE_USER" : "UPDATE_USER", "user:" + user.getUsername());
        return userRepository.findById(user.getId());
    }

    @Transactional
    public void deleteDepartment(Long id, SysUser operator) {
        assertAdmin(operator);
        if (departmentRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "department not found");
        }
        departmentRepository.softDelete(id, operator == null ? null : operator.getId(), LocalDateTime.now());
        operationLogService.save(operator, "SYSTEM", "DELETE_DEPARTMENT", "departmentId:" + id);
    }

    @Transactional
    public void deleteRole(Long id, SysUser operator) {
        assertAdmin(operator);
        if (roleRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "role not found");
        }
        roleRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "SYSTEM", "DELETE_ROLE", "roleId:" + id);
    }

    @Transactional
    public void deleteUser(Long id, SysUser operator) {
        SysUser target = userRepository.findById(id);
        if (target == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "user not found");
        }
        if (!hasRole(operator, "ADMIN")) {
            if (!hasRole(operator, "DEPT_MANAGER")
                || target.getDepartmentId() == null
                || !target.getDepartmentId().equals(operator.getDepartmentId())) {
                throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied");
            }
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "SYSTEM", "DELETE_USER", "userId:" + id);
    }

    private void bindUserRole(Long userId, String roleCode) {
        SysRole role = roleRepository.findByRoleCode(roleCode);
        if (role == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "role not found");
        }
        userRoleRepository.deleteByUserId(userId);
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.insert(userRole);
    }

    private void assertUserManagePermission(Long id, SysUser request, SysUser operator) {
        if (hasRole(operator, "ADMIN")) {
            return;
        }
        if (!hasRole(operator, "DEPT_MANAGER")) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "permission denied");
        }
        Long targetDeptId = request.getDepartmentId();
        if (id != null) {
            SysUser target = userRepository.findById(id);
            if (target == null) {
                throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "user not found");
            }
            targetDeptId = target.getDepartmentId();
        }
        if (targetDeptId == null || operator.getDepartmentId() == null || !targetDeptId.equals(operator.getDepartmentId())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "cannot manage users from other departments");
        }
        if ("ADMIN".equalsIgnoreCase(request.getPrimaryRoleCode())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "cannot assign admin role");
        }
    }

    private void assertAdmin(SysUser operator) {
        if (!hasRole(operator, "ADMIN")) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "admin permission required");
        }
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }
}
