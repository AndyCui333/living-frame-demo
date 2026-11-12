package com.livingframe.framework.security;

import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.model.LoginUser;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 方法级权限判断，供 @PreAuthorize("@ss.hasPermi('...')") 使用。
 */
@Component("ss")
public class PermissionService {

    public boolean hasPermi(String permission) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permission);
    }

    public boolean hasAnyPermi(String... permissions) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        if (loginUser.getPermissions() == null) {
            return false;
        }
        return Arrays.stream(permissions).anyMatch(loginUser.getPermissions()::contains);
    }

    public boolean hasRole(String role) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        return loginUser.getRoles() != null && loginUser.getRoles().contains(role);
    }
}
