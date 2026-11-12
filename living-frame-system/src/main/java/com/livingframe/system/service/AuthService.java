package com.livingframe.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.enums.UserType;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.common.model.LoginUser;
import com.livingframe.system.dto.ChangePasswordRequest;
import com.livingframe.system.dto.LoginRequest;
import com.livingframe.system.entity.SysUser;
import com.livingframe.system.enums.LoginFailureAction;
import com.livingframe.system.mapper.SysPermissionMapper;
import com.livingframe.system.mapper.SysRoleMapper;
import com.livingframe.system.mapper.SysUserMapper;
import com.livingframe.system.util.IpUtils;
import com.livingframe.system.vo.LoginResponse;
import com.livingframe.system.vo.MeResponse;
import com.livingframe.system.vo.PermissionVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 后台认证服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final SysPermissionService permissionService;
    private final LoginSecurityService loginSecurityService;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.username().trim();
        String ip = IpUtils.getClientIp(httpRequest);
        String userAgent = IpUtils.getUserAgent(httpRequest);

        loginSecurityService.assertNotBlocked(username, UserType.ADMIN, ip, userAgent);
        loginSecurityService.assertNotLocked(username, UserType.ADMIN, ip, userAgent);
        try {
            captchaService.validate(request.captchaId(), request.captchaCode());
        } catch (BusinessException ex) {
            loginSecurityService.logFailureOnly(username, UserType.ADMIN, ip, userAgent, ex.getMessage(), null);
            throw ex;
        }

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            LoginFailureAction action = loginSecurityService.recordFailure(
                    username, UserType.ADMIN, ip, userAgent, "用户名或密码错误",
                    user == null ? null : user.getId());
            if (action == LoginFailureAction.BLACKLISTED) {
                throw new BusinessException(403, "登录失败次数过多，当前 IP 已加入系统黑名单");
            }
            if (action == LoginFailureAction.LOCKED) {
                throw new BusinessException(423, "登录失败次数过多，请 5 分钟后再试");
            }
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != Constants.STATUS_ENABLED) {
            loginSecurityService.logFailureOnly(username, UserType.ADMIN, ip, userAgent, "账号已被禁用", user.getId());
            throw new BusinessException("账号已被禁用");
        }

        LoginUser loginUser = buildLoginUser(user);
        StpUtil.login(Constants.ADMIN_LOGIN_ID_PREFIX + user.getId(),
                new SaLoginParameter()
                        .setDevice("admin")
                        .setTimeout(Constants.TOKEN_TIMEOUT_SECONDS));
        StpUtil.getTokenSession().set(Constants.LOGIN_USER_KEY, loginUser);
        loginSecurityService.recordSuccess(username, UserType.ADMIN, ip, userAgent, user.getId());

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return new LoginResponse(
                StpUtil.getTokenValue(),
                StpUtil.getTokenName(),
                StpUtil.getTokenTimeout(),
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                UserType.ADMIN);
    }

    public MeResponse me() {
        LoginUser loginUser = LoginUserContext.require();
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                UserType.ADMIN,
                loginUser.getRoles(),
                loginUser.getPermissions(),
                loginUser.isSuperAdmin());
    }

    public List<PermissionVO> menus() {
        return permissionService.menuTreeForUser(LoginUserContext.require());
    }

    public void logout() {
        StpUtil.logout();
        LoginUserContext.clear();
    }

    public void changePassword(ChangePasswordRequest request) {
        LoginUser loginUser = LoginUserContext.require();
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        try {
            StpUtil.kickout(Constants.ADMIN_LOGIN_ID_PREFIX + user.getId());
        } catch (Exception ex) {
            log.debug("修改密码后踢出用户失败: {}", ex.getMessage());
        }
    }

    public LoginUser buildLoginUser(SysUser user) {
        List<String> roleCodes = roleMapper.selectCodesByUserId(user.getId());
        boolean superAdmin = roleCodes.contains(Constants.ROLE_SUPER_ADMIN);
        List<String> permissionCodes = superAdmin
                ? permissionMapper.selectAllCodes()
                : permissionMapper.selectCodesByUserId(user.getId());
        Set<String> roles = new LinkedHashSet<>(roleCodes);
        Set<String> permissions = new LinkedHashSet<>(permissionCodes);
        return LoginUser.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .userType(UserType.ADMIN)
                .roles(roles)
                .permissions(permissions)
                .superAdmin(superAdmin)
                .build();
    }
}
