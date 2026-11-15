package com.umeed.donations;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against the development database in a transaction that is rolled back.
 * Photos go to a temporary folder that is deleted after each test.
 */
@SpringBootTest(properties = "umeed.storage.local.root=${java.io.tmpdir}/umeed-test-storage")
@Transactional
class DonationApiTests {

    private static final String FIREBASE_UID = "test-uid-donation-api";
    private static final String PHONE = "+919000000002";

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};
    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4};

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

    private static RequestPostProcessor verifiedToken() {
        return jwt().jwt(token -> token.subject(FIREBASE_UID).claim("phone_number", PHONE));
    }

    private void registerDonor(boolean withName) throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(verifiedToken()))
                .andExpect(status().isOk());
        if (withName) {
            mockMvc.perform(put("/api/v1/users/me").with(verifiedToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayName\":\"Asha\"}"))
                    .andExpect(status().isOk());
        }
    }

    private String firstCategoryId() throws Exception {
        String body = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private MockMultipartHttpServletRequestBuilder validForm(String categoryId) {
        MockMultipartHttpServletRequestBuilder form = multipart("/api/v1/donations");
        form.param("categoryId", categoryId)
                .param("title", "Study table")
                .param("description", "Wooden study table in good shape, one drawer.")
                .param("itemCondition", "GOOD")
                .param("city", "Indore")
                .param("area", "Vijay Nagar")
                .param("latitude", "22.75356")
                .param("longitude", "75.89371")
                .param("pickupAddress", "Flat 12, Test Residency, Vijay Nagar, Indore");
        return form;
    }

    private static MockMultipartFile photo(String name, String type, byte[] bytes) {
        return new MockMultipartFile("photos", name, type, bytes);
    }

    @Test
    void postedDonationAppearsInTheFeedAndByIdWithoutPrivateDetails() throws Exception {
        registerDonor(true);
        String categoryId = firstCategoryId();

        MockMultipartHttpServletRequestBuilder form = validForm(categoryId);
        form.file(photo("a.png", "image/png", PNG)).file(photo("b.jpg", "image/jpeg", JPEG));

        String created = mockMvc.perform(form.with(verifiedToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Study table"))
                .andExpect(jsonPath("$.itemCondition").value("GOOD"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.donorName").value("Asha"))
                .andExpect(jsonPath("$.city").value("Indore"))
                .andExpect(jsonPath("$.latitude").value(22.75))
                .andExpect(jsonPath("$.longitude").value(75.89))
                .andExpect(jsonPath("$.imageUrls.length()").value(2))
                .andExpect(jsonPath("$.pickupAddress").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(created, "$.id");
        String firstImageUrl = JsonPath.read(created, "$.imageUrls[0]");

        mockMvc.perform(get("/api/v1/donations").with(verifiedToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem(id)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        mockMvc.perform(get("/api/v1/donations/" + id).with(verifiedToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.imageUrls.length()").value(2))
                .andExpect(jsonPath("$.pickupAddress").doesNotExist());

        mockMvc.perform(get(firstImageUrl).with(verifiedToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void donationWithoutPhotosIsRejected() throws Exception {
        registerDonor(true);
        mockMvc.perform(validForm(firstCategoryId()).with(verifiedToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Add between 1 and 5 photos"));
    }

    @Test
    void fileThatIsNotAnImageIsRejectedEvenIfItClaimsToBeOne() throws Exception {
        registerDonor(true);
        MockMultipartHttpServletRequestBuilder form = validForm(firstCategoryId());
        form.file(photo("fake.png", "image/png", "this is not an image".getBytes(StandardCharsets.UTF_8)));
        mockMvc.perform(form.with(verifiedToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Photos must be JPEG, PNG or WebP images"));
    }

    @Test
    void unknownCategoryIsRejected() throws Exception {
        registerDonor(true);
        MockMultipartHttpServletRequestBuilder form = validForm(UUID.randomUUID().toString());
        form.file(photo("a.png", "image/png", PNG));
        mockMvc.perform(form.with(verifiedToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown category"));
    }

    @Test
    void donorWithoutDisplayNameCannotPost() throws Exception {
        registerDonor(false);
        MockMultipartHttpServletRequestBuilder form = validForm(firstCategoryId());
        form.file(photo("a.png", "image/png", PNG));
        mockMvc.perform(form.with(verifiedToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Complete your profile before posting a donation"));
    }

    @Test
    void invalidFieldsAreReportedPerField() throws Exception {
        registerDonor(true);
        MockMultipartHttpServletRequestBuilder form = multipart("/api/v1/donations");
        form.param("categoryId", firstCategoryId())
                .param("title", "")
                .param("description", "Wooden study table in good shape, one drawer.")
                .param("itemCondition", "GOOD")
                .param("city", "Indore")
                .param("area", "Vijay Nagar")
                .param("latitude", "22.75")
                .param("longitude", "75.89")
                .param("pickupAddress", "Flat 12, Test Residency, Vijay Nagar, Indore");
        form.file(photo("a.png", "image/png", PNG));
        mockMvc.perform(form.with(verifiedToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")));
    }

    @Test
    void unknownDonationAnswersNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/donations/" + UUID.randomUUID()).with(verifiedToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Donation not found"));
    }

    @Test
    void nothingAboutDonationsIsVisibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/donations/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/files/donations/2026/11/anything.png"))
                .andExpect(status().isUnauthorized());
        MockMultipartHttpServletRequestBuilder form = validForm(UUID.randomUUID().toString());
        form.file(photo("a.png", "image/png", PNG));
        mockMvc.perform(form)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingOrInvalidPhotoAddressAnswersNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/files/donations/2026/11/" + UUID.randomUUID() + ".png").with(verifiedToken()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/files/donations/notes.txt").with(verifiedToken()))
                .andExpect(status().isNotFound());
    }
}
