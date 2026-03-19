package com.unequipment.platform.modules.system.service;

import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.finance.entity.Account;
import com.unequipment.platform.modules.finance.repository.AccountRepository;
import com.unequipment.platform.modules.system.dto.LoginRequest;
import com.unequipment.platform.modules.system.dto.RegisterRequest;
import com.unequipment.platform.modules.system.entity.SysDepartment;
import com.unequipment.platform.modules.system.entity.SysRole;
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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.auth.default-department-code:TEST_CENTER}")
    private String defaultDepartmentCode;

    /**
     * 登录流程：
     * 1) 校验用户存在且启用
     * 2) 校验密码
     * 3) 刷新最近登录时间并签发 token
     */
    public Map<String, Object> login(LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.getUsername());
        if (user == null) {
            throw new BizException(ErrorCodes.AUTH_INVALID_CREDENTIALS, "用户名或密码错误");
        }
        if ("PENDING".equalsIgnoreCase(user.getStatus())) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "账号待审核，请联系管理员完成审核");
        }
        if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "账号已停用，请联系管理员");
        }
        String storedPassword = user.getPassword();
        boolean validPassword = storedPassword != null && passwordEncoder.matches(request.getPassword(), storedPassword);
        if (!validPassword) {
            throw new BizException(ErrorCodes.AUTH_INVALID_CREDENTIALS, "用户名或密码错误");
        }
        userRepository.updateLastLoginTime(user.getId(), LocalDateTime.now());
        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenService.generate(user));
        result.put("user", UserView.from(user));
        return result;
    }

    /**
     * 注册流程（事务）：
     * 1) 校验用户名唯一
     * 2) 仅创建校外用户并绑定 EXTERNAL_USER 角色
     * 3) 初始化资金账户
     * 4) 返回申请状态（需管理员审核后登录）
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new BizException(ErrorCodes.AUTH_USERNAME_EXISTS, "用户名已存在");
        }
        SysDepartment department = departmentRepository.findByDeptCode(defaultDepartmentCode);
        if (department == null) {
            department = departmentRepository.findFirst();
        }
        if (department == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "缺少默认部门数据");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setUserType("EXTERNAL");
        user.setPhone(request.getPhone());
        user.setUnitName(request.getUnitName());
        user.setEmail(request.getEmail());
        user.setAuthType("LOCAL");
        user.setDepartmentId(department.getId());
        // 校内账号由管理员批量分发；自助注册仅用于校外用户，需审核后激活。
        user.setStatus("PENDING");
        user.setRemark("校外用户注册申请待审核");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);
        userRepository.insert(user);

        SysRole externalRole = roleRepository.findByRoleCode("EXTERNAL_USER");
        if (externalRole == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "缺少默认角色数据(EXTERNAL_USER)");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(externalRole.getId());
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.insert(userRole);
        user.setPrimaryRoleCode("EXTERNAL_USER");
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
        result.put("auditStatus", "PENDING");
        result.put("message", "注册申请已提交，请等待管理员审核通过后登录");
        result.put("user", UserView.from(user));
        return result;
    }

    /**
     * 获取当前登录用户信息。
     * 说明：
     * 1) 通过认证过滤器注入的 currentUser 获取用户主键
     * 2) 再次查询数据库，确保返回最新的角色/部门等扩展字段
     */
    public UserView info(SysUser currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或登录已失效");
        }
        SysUser dbUser = userRepository.findById(currentUser.getId());
        if (dbUser == null || !"ENABLED".equalsIgnoreCase(dbUser.getStatus())) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "用户不存在或已停用");
        }
        return UserView.from(dbUser);
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
