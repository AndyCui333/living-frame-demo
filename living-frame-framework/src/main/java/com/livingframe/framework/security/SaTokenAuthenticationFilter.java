package com.livingframe.framework.security;

import cn.dev33.satoken.stp.StpUtil;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.model.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 sa-token 读取登录态并写入 Spring Security 上下文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean bound = RequestContextHolder.getRequestAttributes() == null;
        if (bound) {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        }
        try {
            authenticate();
            filterChain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
            if (bound) {
                RequestContextHolder.resetRequestAttributes();
            }
        }
    }

    private void authenticate() {
        try {
            if (!StpUtil.isLogin()) {
                return;
            }
            StpUtil.checkActiveTimeout();
            Object value = StpUtil.getTokenSession()
                    .get(Constants.LOGIN_USER_KEY);
            if (!(value instanceof LoginUser loginUser)) {
                return;
            }
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (loginUser.getRoles() != null) {
                loginUser.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
            if (loginUser.getPermissions() != null) {
                loginUser.getPermissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            LoginUserContext.set(loginUser);
        } catch (Exception ex) {
            log.debug("sa-token 认证失败: {}", ex.getMessage());
        }
    }
}
