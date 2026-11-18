package com.umeed.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Simple limits against floods and spam (H02). Runs after the login check, so the account is known:
 * every request counts against the caller's address, every change (POST, PUT, PATCH, DELETE) against the
 * account, and posting an item with photos has its own lower limit. Over a limit the answer is 429.
 */
class RateLimitInterceptor implements HandlerInterceptor {

    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");
    private static final String UPLOAD_PATH = "/api/v1/donations";

    private final RateLimiter limiter;
    private final int writesPerMinute;
    private final int uploadsPerMinute;
    private final int requestsPerIpPerMinute;

    RateLimitInterceptor(RateLimiter limiter, int writesPerMinute, int uploadsPerMinute, int requestsPerIpPerMinute) {
        this.limiter = limiter;
        this.writesPerMinute = writesPerMinute;
        this.uploadsPerMinute = uploadsPerMinute;
        this.requestsPerIpPerMinute = requestsPerIpPerMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Behind Nginx the remote address is the client's, because prod trusts X-Forwarded-For.
        if (!limiter.tryAcquire("ip:" + request.getRemoteAddr(), requestsPerIpPerMinute)) {
            throw tooMany();
        }
        String method = request.getMethod();
        String account = accountOf(SecurityContextHolder.getContext().getAuthentication());
        if (account == null || READS.contains(method)) {
            return true;
        }
        if (!limiter.tryAcquire("write:" + account, writesPerMinute)) {
            throw tooMany();
        }
        if ("POST".equals(method) && UPLOAD_PATH.equals(request.getRequestURI())
                && !limiter.tryAcquire("upload:" + account, uploadsPerMinute)) {
            throw tooMany();
        }
        return true;
    }

    private static String accountOf(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof Jwt jwt ? jwt.getSubject() : null;
    }

    private static ResponseStatusException tooMany() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait a minute and try again.");
    }
}
