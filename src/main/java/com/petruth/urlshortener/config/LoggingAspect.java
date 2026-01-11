package com.petruth.urlshortener.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.petruth.urlshortener.controller..*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        // Generate correlation ID
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";
        String ip = request != null ? getClientIP(request) : "UNKNOWN";

        long startTime = System.currentTimeMillis();

        log.info("🔵 REQUEST  | {} {} | IP: {} | User-Agent: {} | Correlation-ID: {}",
                method, uri, ip, userAgent, correlationId);

        Object result = null;
        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("🟢 RESPONSE | {} {} | Duration: {}ms | Status: SUCCESS | Correlation-ID: {}",
                    method, uri, duration, correlationId);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            log.error("🔴 ERROR    | {} {} | Duration: {}ms | Error: {} | Correlation-ID: {}",
                    method, uri, duration, e.getMessage(), correlationId, e);

            throw e;
        } finally {
            MDC.clear();
        }
    }

    @Around("execution(* com.petruth.urlshortener.service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        log.debug("📘 SERVICE  | {}.{} | Started", className, methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.debug("📗 SERVICE  | {}.{} | Completed in {}ms",
                    className, methodName, duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            log.error("📕 SERVICE  | {}.{} | Failed after {}ms | Error: {}",
                    className, methodName, duration, e.getMessage(), e);

            throw e;
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}