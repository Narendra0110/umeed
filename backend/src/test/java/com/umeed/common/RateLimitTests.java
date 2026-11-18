package com.umeed.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The rate limits (H02) with small numbers, so a few requests reach them. The other test classes run with
 * the limits off (pom.xml). Runs against the development database in a rolled-back transaction.
 */
@SpringBootTest(properties = {
        "umeed.rate-limit.enabled=true",
        "umeed.rate-limit.writes-per-minute=3",
        "umeed.rate-limit.uploads-per-minute=1",
        "umeed.rate-limit.requests-per-ip-per-minute=20"})
@Transactional
class RateLimitTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static RequestPostProcessor user(String uid, String phone, String address) {
        RequestPostProcessor token = jwt().jwt(t -> t.subject(uid).claim("phone_number", phone));
        return request -> {
            request.setRemoteAddr(address);
            return token.postProcessRequest(request);
        };
    }

    private ResultActions setLanguage(RequestPostProcessor who, String language) throws Exception {
        return mockMvc.perform(put("/api/v1/users/me/language").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"" + language + "\"}"));
    }

    @Test
    void anAccountCanMakeAFewChangesAMinuteThenGets429() throws Exception {
        RequestPostProcessor asha = user("test-uid-rate-asha", "+919000000111", "10.0.0.1");
        RequestPostProcessor bilal = user("test-uid-rate-bilal", "+919000000112", "10.0.0.2");
        mockMvc.perform(get("/api/v1/users/me").with(asha)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me").with(bilal)).andExpect(status().isOk());

        setLanguage(asha, "hi").andExpect(status().isOk());
        setLanguage(asha, "en").andExpect(status().isOk());
        setLanguage(asha, "hi").andExpect(status().isOk());
        setLanguage(asha, "en")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many requests. Please wait a minute and try again."));
        mockMvc.perform(put("/api/v1/users/me/language").with(asha)
                        .header("Accept-Language", "hi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"en\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("बहुत ज़्यादा अनुरोध हो गए। कृपया एक मिनट रुककर फिर से कोशिश करें।"));

        // Reading is not limited per account, and another account has its own count.
        mockMvc.perform(get("/api/v1/users/me").with(asha)).andExpect(status().isOk());
        setLanguage(bilal, "hi").andExpect(status().isOk());
    }

    @Test
    void oneAddressCanMakeOnlySoManyRequestsAMinute() throws Exception {
        RequestPostProcessor esha = user("test-uid-rate-esha", "+919000000113", "10.0.0.3");
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/v1/categories").with(esha)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/categories").with(esha)).andExpect(status().isTooManyRequests());
        mockMvc.perform(get("/api/v1/categories").with(user("test-uid-rate-dev", "+919000000114", "10.0.0.4")))
                .andExpect(status().isOk());
    }

    @Test
    void onlyTheHealthCheckOfTheActuatorIsReachable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        RequestPostProcessor someone = user("test-uid-rate-dev", "+919000000114", "10.0.0.5");
        mockMvc.perform(get("/actuator/env").with(someone)).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans").with(someone)).andExpect(status().isNotFound());
    }

    @Test
    void countsStartAgainEachMinute() {
        AtomicLong now = new AtomicLong(Instant.parse("2026-11-18T10:00:05Z").toEpochMilli());
        Clock clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(now.get());
            }
        };
        RateLimiter limiter = new RateLimiter(clock);
        assertTrue(limiter.tryAcquire("k", 2));
        assertTrue(limiter.tryAcquire("k", 2));
        assertFalse(limiter.tryAcquire("k", 2));
        now.addAndGet(60_000);
        assertTrue(limiter.tryAcquire("k", 2));
    }
}
