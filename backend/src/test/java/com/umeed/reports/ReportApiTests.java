package com.umeed.reports;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against the development database in a transaction that is rolled back.
 * Photos go to a temporary folder that is deleted after each test.
 */
@SpringBootTest(properties = "umeed.storage.local.root=${java.io.tmpdir}/umeed-test-storage")
@Transactional
class ReportApiTests {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private String donationId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        register(donor(), "Dev");
        register(asha(), "Asha");
        donationId = donorPostsDonation();
    }

    @AfterEach
    void removeStoredTestFiles() throws IOException {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "umeed-test-storage");
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    private static RequestPostProcessor donor() {
        return user("test-uid-report-donor", "+919000000031");
    }

    private static RequestPostProcessor asha() {
        return user("test-uid-report-asha", "+919000000032");
    }

    private static RequestPostProcessor user(String uid, String phone) {
        return jwt().jwt(token -> token.subject(uid).claim("phone_number", phone));
    }

    private void register(RequestPostProcessor token, String name) throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(token))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/users/me").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + name + "\"}"))
                .andExpect(status().isOk());
    }

    private String donorPostsDonation() throws Exception {
        String categories = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String categoryId = JsonPath.read(categories, "$[0].id");

        MockMultipartHttpServletRequestBuilder form = multipart("/api/v1/donations");
        form.param("categoryId", categoryId)
                .param("title", "Winter jacket")
                .param("description", "Posted by an automated test, in good condition.")
                .param("itemCondition", "GOOD")
                .param("city", "Test city")
                .param("area", "Test area")
                .param("latitude", "22.75")
                .param("longitude", "75.89")
                .param("pickupAddress", "Flat 12, Test Residency, Test area, Test city");
        form.file(new MockMultipartFile("photos", "a.png", "image/png", PNG));
        String created = mockMvc.perform(form.with(donor()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private ResultActions report(String id, RequestPostProcessor token, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/donations/" + id + "/reports").with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void aUserCanReportAListingOnceAndItStaysVisible() throws Exception {
        report(donationId, asha(), "{\"reason\":\"SELLING_OR_ASKING_MONEY\",\"details\":\"Asked me for 500 rupees\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donationId").value(donationId))
                .andExpect(jsonPath("$.reason").value("SELLING_OR_ASKING_MONEY"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reporterId").doesNotExist());

        report(donationId, asha(), "{\"reason\":\"OFFENSIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have already reported this listing"));

        // No automatic hiding: the listing is still there until someone reviews the report.
        mockMvc.perform(get("/api/v1/donations/" + donationId).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void reportRulesAreEnforced() throws Exception {
        report(donationId, donor(), "{\"reason\":\"OFFENSIVE\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot report your own listing"));

        report(donationId, asha(), "{\"reason\":\"OTHER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please describe the problem"));

        report(donationId, asha(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));

        report(donationId, asha(), "{\"reason\":\"NOT_A_REASON\"}")
                .andExpect(status().isBadRequest());

        report(donationId, asha(), "{\"reason\":\"OTHER\",\"details\":\"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("details"));

        report(UUID.randomUUID().toString(), asha(), "{\"reason\":\"OFFENSIVE\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Donation not found"));

        // A reason other than OTHER needs no details.
        report(donationId, asha(), "{\"reason\":\"PROHIBITED_ITEM\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void aNeedCanBeReportedOnceButNotByThePersonWhoAsked() throws Exception {
        String categories = mockMvc.perform(get("/api/v1/categories"))
                .andReturn().getResponse().getContentAsString();
        String created = mockMvc.perform(post("/api/v1/needs").with(donor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","title":"Rain boots","description":"Size 5, any colour.",
                                 "city":"Test city","area":"Test area","latitude":22.75,"longitude":75.89}
                                """.formatted((String) JsonPath.read(categories, "$[0].id"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String needId = JsonPath.read(created, "$.id");
        String path = "/api/v1/needs/" + needId + "/reports";

        mockMvc.perform(post(path).with(asha()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FAKE_OR_MISLEADING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.needId").value(needId))
                .andExpect(jsonPath("$.donationId").doesNotExist())
                .andExpect(jsonPath("$.status").value("OPEN"));
        mockMvc.perform(post(path).with(asha()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"OFFENSIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have already reported this need"));
        mockMvc.perform(post(path).with(donor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"OFFENSIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot report your own need"));
        mockMvc.perform(post("/api/v1/needs/" + UUID.randomUUID() + "/reports").with(asha())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"OFFENSIVE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reportingNeedsALogin() throws Exception {
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"OFFENSIVE\"}"))
                .andExpect(status().isUnauthorized());
    }
}
