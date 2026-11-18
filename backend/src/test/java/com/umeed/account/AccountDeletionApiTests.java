package com.umeed.account;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class AccountDeletionApiTests {

    private static final String DONOR_PHONE = "+919000000051";
    private static final String ADDRESS = "Flat 12, Test Residency, Test area, Test city";
    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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
        return user("test-uid-delete-donor", DONOR_PHONE);
    }

    /** The same phone number after a fresh Firebase sign-up, which gives a new Firebase id. */
    private static RequestPostProcessor donorSignedUpAgain() {
        return user("test-uid-delete-donor-again", DONOR_PHONE);
    }

    private static RequestPostProcessor asha() {
        return user("test-uid-delete-asha", "+919000000052");
    }

    private static RequestPostProcessor bilal() {
        return user("test-uid-delete-bilal", "+919000000053");
    }

    private static RequestPostProcessor user(String uid, String phone) {
        return jwt().jwt(token -> token.subject(uid).claim("phone_number", phone));
    }

    private String register(RequestPostProcessor token, String name) throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(token)).andExpect(status().isOk());
        String body = mockMvc.perform(put("/api/v1/users/me").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String postItem(RequestPostProcessor token, String title) throws Exception {
        String categories = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String categoryId = JsonPath.read(categories, "$[0].id");
        MockMultipartHttpServletRequestBuilder form = multipart("/api/v1/donations");
        form.param("categoryId", categoryId)
                .param("title", title)
                .param("description", "Posted by an automated test, in good condition.")
                .param("itemCondition", "GOOD")
                .param("city", "Test city")
                .param("area", "Test area")
                .param("latitude", "22.75")
                .param("longitude", "75.89")
                .param("pickupAddress", ADDRESS);
        form.file(new MockMultipartFile("photos", "a.png", "image/png", PNG));
        String created = mockMvc.perform(form.with(token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private String claim(String donationId, RequestPostProcessor token, String message) throws Exception {
        String body = mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void accept(String claimId) throws Exception {
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").with(donor()))
                .andExpect(status().isOk());
    }

    private void confirm(String donationId, RequestPostProcessor token) throws Exception {
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/pickup/confirm").with(token))
                .andExpect(status().isOk());
    }

    @Test
    void deletingAnAccountErasesItsPrivateData() throws Exception {
        String donorId = register(donor(), "Dev");
        String ashaId = register(asha(), "Asha");
        register(bilal(), "Bilal");

        // An available item with a pending claim from Bilal.
        String available = postItem(donor(), "Available lamp");
        claim(available, bilal(), "Bilal wants it");
        // An item already handed over to Asha.
        String given = postItem(donor(), "Given books");
        accept(claim(given, asha(), "Asha wants it"));
        confirm(given, donor());
        confirm(given, asha());
        // The donor's own pending claim on Asha's item, and a block.
        String ashaItem = postItem(asha(), "Asha's chair");
        claim(ashaItem, donor(), "My phone is in my message");
        mockMvc.perform(post("/api/v1/blocks").with(donor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + ashaId + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/users/me").with(donor()))
                .andExpect(status().isNoContent());

        // The user row keeps no identity.
        Map<String, Object> userRow = jdbcTemplate.queryForMap(
                "select firebase_uid, phone_number, display_name, status, deleted_at from users where id = ?::uuid",
                donorId);
        assertNull(userRow.get("firebase_uid"));
        assertNull(userRow.get("phone_number"));
        assertNull(userRow.get("display_name"));
        assertEquals("DELETED", userRow.get("status"));
        assertNotNull(userRow.get("deleted_at"));

        // Every pickup address is erased; the available item is removed with its photos; the given one stays.
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from donations where donor_id = ?::uuid and pickup_address is not null",
                Integer.class, donorId));
        assertEquals("REMOVED", jdbcTemplate.queryForObject(
                "select status from donations where id = ?::uuid", String.class, available));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from donation_images where donation_id = ?::uuid", Integer.class, available));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from donation_images where donation_id = ?::uuid", Integer.class, given));

        // Bilal's claim on the removed item is closed. The donor's own claim is withdrawn and its message erased.
        mockMvc.perform(get("/api/v1/claims/mine").with(bilal()))
                .andExpect(jsonPath("$.items[?(@.donationId == '" + available + "')].status", contains("REJECTED")));
        Map<String, Object> ownClaim = jdbcTemplate.queryForMap(
                "select status, message from claims where claimant_id = ?::uuid", donorId);
        assertEquals("WITHDRAWN", ownClaim.get("status"));
        assertNull(ownClaim.get("message"));

        // Blocks are gone, so Asha's list is unaffected and no row mentions the deleted user.
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from user_blocks where blocker_id = ?::uuid or blocked_id = ?::uuid",
                Integer.class, donorId, donorId));

        // Asha still sees her received item, now from "Umeed user", and no phone number for the donor.
        mockMvc.perform(get("/api/v1/donations/" + given).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorName").value("Umeed user"));
        mockMvc.perform(get("/api/v1/donations/" + given + "/pickup").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donor.phoneNumber", nullValue()))
                .andExpect(jsonPath("$.donor.name").value("Umeed user"))
                .andExpect(jsonPath("$.pickupAddress", nullValue()));

        // The old sign-in can no longer act.
        mockMvc.perform(get("/api/v1/donations/mine").with(donor()))
                .andExpect(status().isNotFound());
    }

    @Test
    void theSameNumberCanRegisterAgainAsANewAccount() throws Exception {
        String oldId = register(donor(), "Dev");
        mockMvc.perform(delete("/api/v1/users/me").with(donor()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").with(donorSignedUpAgain()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(not(oldId)))
                .andExpect(jsonPath("$.phoneNumber").value(DONOR_PHONE))
                .andExpect(jsonPath("$.profileComplete").value(false));
    }

    @Test
    void deletionIsRefusedWhileAPickupIsInProgress() throws Exception {
        String donorId = register(donor(), "Dev");
        String ashaId = register(asha(), "Asha");
        String item = postItem(donor(), "Reserved lamp");
        accept(claim(item, asha(), "Please"));

        // Neither the donor nor the receiver can delete their account now.
        mockMvc.perform(delete("/api/v1/users/me").with(donor()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/users/me").with(asha()))
                .andExpect(status().isConflict());

        // Nothing was changed by the refused attempts.
        assertEquals("ACTIVE", jdbcTemplate.queryForObject(
                "select status from users where id = ?::uuid", String.class, donorId));
        assertEquals("ACTIVE", jdbcTemplate.queryForObject(
                "select status from users where id = ?::uuid", String.class, ashaId));
        assertEquals(ADDRESS, jdbcTemplate.queryForObject(
                "select pickup_address from donations where id = ?::uuid", String.class, item));

        // Once the pickup is cancelled, the receiver can delete their account.
        mockMvc.perform(post("/api/v1/donations/" + item + "/pickup/cancel").with(asha()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/users/me").with(asha()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletingNeedsALogin() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
