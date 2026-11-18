package com.umeed.users;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against the development database. Every test runs in a transaction that is rolled back,
 * so no test user is left behind.
 */
@SpringBootTest
@Transactional
class UserApiTests {

    private static final String FIREBASE_UID = "test-uid-user-api";
    private static final String PHONE = "+919000000001";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static RequestPostProcessor verifiedToken() {
        return jwt().jwt(token -> token.subject(FIREBASE_UID).claim("phone_number", PHONE));
    }

    @Test
    void firstCallRegistersTheUserAndLaterCallsFindTheSameUser() throws Exception {
        String first = mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(PHONE))
                .andExpect(jsonPath("$.displayName").doesNotExist())
                .andExpect(jsonPath("$.profileComplete").value(false))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals((String) JsonPath.read(first, "$.id"), (String) JsonPath.read(second, "$.id"));
    }

    @Test
    void languageStartsAsEnglishAndCanBeChanged() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));

        mockMvc.perform(put("/api/v1/users/me/language").with(verifiedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("hi"));
        mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(jsonPath("$.language").value("hi"));

        mockMvc.perform(put("/api/v1/users/me/language").with(verifiedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"fr\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("language"));
        mockMvc.perform(put("/api/v1/users/me/language").with(verifiedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void messagesComeInHindiWhenTheAppAsksForHindi() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token.subject(FIREBASE_UID)))
                        .header("Accept-Language", "hi"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("फ़ोन नंबर से लॉग इन करना ज़रूरी है"));

        mockMvc.perform(get("/api/v1/users/me").with(verifiedToken())).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/users/me/language").with(verifiedToken())
                        .header("Accept-Language", "hi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"fr\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("कुछ जानकारी सही नहीं है"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("भाषा en या hi होनी चाहिए"));

        // English when asked for English, or when nothing is asked.
        mockMvc.perform(put("/api/v1/users/me/language").with(verifiedToken())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"fr\"}"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Language must be en or hi"));
    }

    @Test
    void tokenWithoutPhoneNumberIsRefused() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token.subject(FIREBASE_UID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Phone sign-in is required"));
    }

    @Test
    void displayNameCanBeSetAndIsTrimmed() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/me").with(verifiedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  Asha  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha"))
                .andExpect(jsonPath("$.profileComplete").value(true));

        mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha"));
    }

    @Test
    void emptyDisplayNameIsRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/me").with(verifiedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("displayName"));
    }

    @Test
    void brokenBodyIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/users/me").with(verifiedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void settingANameBeforeTheFirstLoginCallAnswersNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/users/me").with(verifiedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Asha\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
