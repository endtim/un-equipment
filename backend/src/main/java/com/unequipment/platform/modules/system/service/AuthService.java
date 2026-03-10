package com.unequipment.platform.modules.system.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.system.dto.LoginRequest;
import com.unequipment.platform.modules.system.dto.RegisterRequest;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.entity.SysUserRole;
import com.unequipment.platform.modules.system.repository.SysDepartmentRepository;
import com.unequipment.platform.modules.system.repository.SysRoleRepository;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import com.unequipment.platform.modules.system.repository.SysUserRoleRepository;
import com.unequipment.platform.security.TokenService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final SysDepartmentRepository departmentRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final AccountRepository accountRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> login(LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.getUsername());
        if (user == null || !"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw new BizException("username or password is invalid");
        }
        String storedPassword = user.getPassword();
        boolean validPassword = storedPassword != null && passwordEncoder.matches(request.getPassword(), storedPassword);
        if (!validPassword && storedPassword != null && !storedPassword.startsWith("$2")) {
            // legacy plain-text password compatibility: login once then upgrade to BCrypt hash
            validPassword = storedPassword.equals(request.getPassword());
            if (validPassword) {
                userRepository.updatePassword(user.getId(), passwordEncoder.encode(request.getPassword()), LocalDateTime.now());
            }
        }
        if (!validPassword) {
            throw new BizException("username or password is invalid");
        }
        userRepository.updateLastLoginTime(user.getId(), LocalDateTime.now());
        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenService.generate(user));
        result.put("user", UserView.from(user));
        return result;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new BizException("username already exists");
        }
        SysDepartment department = departmentRepository.findFirst();
        if (department == null) {
            throw new BizException("department seed data missing");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setUserType("INTERNAL");
        user.setAuthType("LOCAL");
        user.setDepartmentId(department.getId());
        user.setStatus("ENABLED");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);
        userRepository.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(roleRepository.findByRoleCode("INTERNAL_USER").getId());
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.insert(userRole);
        user.setPrimaryRoleCode("INTERNAL_USER");
        user.setDepartmentName(department.getDeptName());

        Account account = new Account();
        account.setUserId(user.getId());
        account.setBalance(BigDecimal.ZERO);
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setTotalRecharge(BigDecimal.ZERO);
        account.setTotalConsume(BigDecimal.ZERO);
        account.setStatus("ENABLED");
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        accountRepository.insert(account);

        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenService.generate(user));
        result.put("user", UserView.from(user));
        return result;
    }

    @lombok.Value
    public static class UserView {
        Long id;
        String username;
        String realName;
        String roleCode;
        String departmentName;

        public static UserView from(SysUser user) {
            return new UserView(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPrimaryRoleCode(),
                user.getDepartmentName()
            );
        }
    }
}
