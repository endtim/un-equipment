package com.unequipment.platform.common.util;

import com.unequipment.platform.modules.system.entity.SysUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class RoleAuthUtils {

    private RoleAuthUtils() {
    }

    public static boolean hasRole(SysUser user, String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return false;
        }
        String normalizedRole = roleCode.trim().toUpperCase();
        if (user != null && user.getPrimaryRoleCode() != null
            && normalizedRole.equalsIgnoreCase(user.getPrimaryRoleCode())) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String authority = "ROLE_" + normalizedRole;
        for (GrantedAuthority item : authentication.getAuthorities()) {
            if (item != null && authority.equalsIgnoreCase(item.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

