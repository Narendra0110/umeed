package com.umeed.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

/** Turns the rate limits on for every /api request, unless umeed.rate-limit.enabled is false. */
@Configuration
class RateLimitConfig implements WebMvcConfigurer {

    private final boolean enabled;
    private final int writesPerMinute;
    private final int uploadsPerMinute;
    private final int requestsPerIpPerMinute;

    RateLimitConfig(
            @Value("${umeed.rate-limit.enabled:true}") boolean enabled,
            @Value("${umeed.rate-limit.writes-per-minute:30}") int writesPerMinute,
            @Value("${umeed.rate-limit.uploads-per-minute:10}") int uploadsPerMinute,
            @Value("${umeed.rate-limit.requests-per-ip-per-minute:300}") int requestsPerIpPerMinute) {
        this.enabled = enabled;
        this.writesPerMinute = writesPerMinute;
        this.uploadsPerMinute = uploadsPerMinute;
        this.requestsPerIpPerMinute = requestsPerIpPerMinute;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (enabled) {
            registry.addInterceptor(new RateLimitInterceptor(
                            new RateLimiter(Clock.systemUTC()), writesPerMinute, uploadsPerMinute, requestsPerIpPerMinute))
                    .addPathPatterns("/api/**");
        }
    }
}
