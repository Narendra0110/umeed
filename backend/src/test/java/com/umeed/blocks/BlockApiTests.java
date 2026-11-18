package com.umeed.blocks;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class BlockApiTests {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};

    @Autowired
    private WebApplicationContext context;

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
        return user("test-uid-block-donor", "+919000000041");
    }

    private static RequestPostProcessor asha() {
        return user("test-uid-block-asha", "+919000000042");
    }

    private static RequestPostProcessor bilal() {
        return user("test-uid-block-bilal", "+919000000043");
    }

    private static RequestPostProcessor user(String uid, String phone) {
        return jwt().jwt(token -> token.subject(uid).claim("phone_number", phone));
    }

    /** Registers a user with a name and returns their user id. */
    private String register(RequestPostProcessor token, String name) throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(token)).andExpect(status().isOk());
        String body = mockMvc.perform(put("/api/v1/users/me").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Posts one donation as the given user. Returns the donation id. */
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
                .param("pickupAddress", "Flat 12, Test Residency, Test area, Test city");
        form.file(new MockMultipartFile("photos", "a.png", "image/png", PNG));
        String created = mockMvc.perform(form.with(token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private String claim(String donationId, RequestPostProcessor token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Please\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void block(RequestPostProcessor token, String userId, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/blocks").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private List<String> feedIds(RequestPostProcessor token, String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/donations" + query).with(token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    @Test
    void aBlockHidesItemsBothWaysUntilUnblocked() throws Exception {
        register(donor(), "Dev");
        String ashaId = register(asha(), "Asha");
        String donorItem = postItem(donor(), "Donor's lamp");
        String ashaItem = postItem(asha(), "Asha's books");

        assertTrue(feedIds(asha(), "?size=50").contains(donorItem));

        // The donor blocks Asha: neither sees the other's items, in either feed order.
        block(donor(), ashaId, 204);
        assertFalse(feedIds(asha(), "?size=50").contains(donorItem), "blocked person sees blocker's item");
        assertFalse(feedIds(asha(), "?lat=22.72&lng=75.86&size=50").contains(donorItem));
        assertFalse(feedIds(donor(), "?size=50").contains(ashaItem), "blocker sees blocked person's item");
        assertFalse(feedIds(donor(), "?lat=22.72&lng=75.86&size=50").contains(ashaItem));

        // Neither can claim the other's items.
        mockMvc.perform(post("/api/v1/donations/" + donorItem + "/claims").with(asha())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/donations/" + ashaItem + "/claims").with(donor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        // The block is listed for the blocker only, with a name and no phone number.
        mockMvc.perform(get("/api/v1/blocks").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(ashaId))
                .andExpect(jsonPath("$[0].name").value("Asha"))
                .andExpect(jsonPath("$[0].phoneNumber").doesNotExist());
        mockMvc.perform(get("/api/v1/blocks").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Blocking twice changes nothing.
        block(donor(), ashaId, 204);

        // Only the blocker can lift the block.
        mockMvc.perform(delete("/api/v1/blocks/" + ashaId).with(donor()))
                .andExpect(status().isNoContent());
        assertTrue(feedIds(asha(), "?size=50").contains(donorItem));
        assertTrue(feedIds(donor(), "?size=50").contains(ashaItem));
    }

    @Test
    void blockingClosesOpenClaimsBetweenTheTwo() throws Exception {
        register(donor(), "Dev");
        String ashaId = register(asha(), "Asha");
        register(bilal(), "Bilal");
        String donorItem = postItem(donor(), "Donor's lamp");
        String ashaItem = postItem(asha(), "Asha's books");
        claim(donorItem, asha());
        claim(donorItem, bilal());
        claim(ashaItem, donor());

        block(donor(), ashaId, 204);

        // Asha's claim on the donor's item is rejected; Bilal's is untouched.
        mockMvc.perform(get("/api/v1/donations/" + donorItem + "/claims").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.claimantName == 'Asha')].status", contains("REJECTED")))
                .andExpect(jsonPath("$[?(@.claimantName == 'Bilal')].status", contains("PENDING")));
        // The donor's own claim on Asha's item is withdrawn.
        mockMvc.perform(get("/api/v1/donations/" + ashaItem + "/claims").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.claimantName == 'Dev')].status", contains("WITHDRAWN")));
    }

    @Test
    void blockingIsRefusedWhileTheTwoShareAPickup() throws Exception {
        String donorId = register(donor(), "Dev");
        String ashaId = register(asha(), "Asha");
        String item = postItem(donor(), "Donor's lamp");
        String ashaClaim = claim(item, asha());
        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/accept").with(donor()))
                .andExpect(status().isOk());

        block(donor(), ashaId, 409);
        block(asha(), donorId, 409);

        // After the pickup is cancelled, blocking works.
        mockMvc.perform(post("/api/v1/donations/" + item + "/pickup/cancel").with(donor()))
                .andExpect(status().isNoContent());
        block(asha(), donorId, 204);
    }

    @Test
    void cannotBlockYourselfOrSomeoneWhoDoesNotExist() throws Exception {
        String donorId = register(donor(), "Dev");
        block(donor(), donorId, 400);
        block(donor(), UUID.randomUUID().toString(), 404);
        mockMvc.perform(post("/api/v1/blocks").with(donor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
