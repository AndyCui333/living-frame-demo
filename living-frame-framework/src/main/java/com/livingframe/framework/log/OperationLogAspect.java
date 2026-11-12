package com.livingframe.framework.log;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.model.LoginUser;
import com.livingframe.system.service.OperationLogService;
import com.livingframe.system.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 记录标注了 @OperationLog 的接口操作结果。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            status = "FAIL";
            errorMessage = throwable.getMessage();
            throw throwable;
        } finally {
            try {
                HttpServletRequest request = currentRequest();
                LoginUser loginUser = LoginUserContext.get();
                operationLogService.record(
                        loginUser == null ? null : loginUser.getId(),
                        loginUser == null ? "anonymous" : loginUser.getUsername(),
                        operationLog.value(),
                        request == null ? null : request.getMethod(),
                        request == null ? null : request.getRequestURI(),
                        request == null ? null : IpUtils.getClientIp(request),
                        status,
                        errorMessage,
                        System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("记录操作日志失败: {}", ex.getMessage());
            }
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
