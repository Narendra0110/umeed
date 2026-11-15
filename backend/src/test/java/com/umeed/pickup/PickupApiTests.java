package com.umeed.pickup;

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
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
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
class PickupApiTests {

    private static final String DONOR_PHONE = "+919000000021";
    private static final String ASHA_PHONE = "+919000000022";
    private static final String ADDRESS = "Flat 12, Test Residency, Test area, Test city";

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private String donationId;
    private String ashaClaim;
    private String bilalClaim;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        register(donor(), "Dev");
        register(asha(), "Asha");
        register(bilal(), "Bilal");
        register(chitra(), "Chitra");
        donationId = donorPostsDonation();
        ashaClaim = claim(asha());
        bilalClaim = claim(bilal());
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
        return user("test-uid-pickup-donor", DONOR_PHONE);
    }

    private static RequestPostProcessor asha() {
        return user("test-uid-pickup-asha", ASHA_PHONE);
    }

    private static RequestPostProcessor bilal() {
        return user("test-uid-pickup-bilal", "+919000000023");
    }

    private static RequestPostProcessor chitra() {
        return user("test-uid-pickup-chitra", "+919000000024");
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
                .param("pickupAddress", ADDRESS);
        form.file(new MockMultipartFile("photos", "a.png", "image/png", PNG));
        String created = mockMvc.perform(form.with(donor()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private String claim(RequestPostProcessor token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void donorAccepts(String claimId) throws Exception {
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    private String pickupUrl() {
        return "/api/v1/donations/" + donationId + "/pickup";
    }

    @Test
    void onlyTheDonorAndTheAcceptedReceiverSeeTheAddressAndPhoneNumbers() throws Exception {
        // Before any claim is accepted, nobody gets the details.
        mockMvc.perform(get(pickupUrl()).with(donor()))
                .andExpect(status().isConflict());
        mockMvc.perform(get(pickupUrl()).with(asha()))
                .andExpect(status().isForbidden());

        donorAccepts(ashaClaim);

        mockMvc.perform(get(pickupUrl()).with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.donationTitle").value("Winter jacket"))
                .andExpect(jsonPath("$.pickupAddress").value(ADDRESS))
                .andExpect(jsonPath("$.donor.name").value("Dev"))
                .andExpect(jsonPath("$.donor.phoneNumber").value(DONOR_PHONE))
                .andExpect(jsonPath("$.receiver.name").value("Asha"))
                .andExpect(jsonPath("$.receiver.phoneNumber").value(ASHA_PHONE))
                .andExpect(jsonPath("$.donorConfirmed").value(false))
                .andExpect(jsonPath("$.receiverConfirmed").value(false));

        mockMvc.perform(get(pickupUrl()).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupAddress").value(ADDRESS))
                .andExpect(jsonPath("$.donor.phoneNumber").value(DONOR_PHONE));

        // A rejected claimant and an unrelated user are refused, and so is anyone without a login.
        mockMvc.perform(get(pickupUrl()).with(bilal()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.pickupAddress").doesNotExist());
        mockMvc.perform(get(pickupUrl()).with(chitra()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(pickupUrl()))
                .andExpect(status().isUnauthorized());

        // The public donation view still shows no private details.
        mockMvc.perform(get("/api/v1/donations/" + donationId).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupAddress").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist());
    }

    @Test
    void theDonationIsCompletedOnlyWhenBothSidesConfirm() throws Exception {
        donorAccepts(ashaClaim);

        mockMvc.perform(post(pickupUrl() + "/confirm").with(bilal()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(pickupUrl() + "/confirm").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorConfirmed").value(true))
                .andExpect(jsonPath("$.receiverConfirmed").value(false))
                .andExpect(jsonPath("$.status").value("RESERVED"));

        // Confirming twice changes nothing.
        mockMvc.perform(post(pickupUrl() + "/confirm").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(post(pickupUrl() + "/confirm").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorConfirmed").value(true))
                .andExpect(jsonPath("$.receiverConfirmed").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/donations/" + donationId).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // A completed donation cannot be cancelled or claimed.
        mockMvc.perform(post(pickupUrl() + "/cancel").with(donor()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(chitra()))
                .andExpect(status().isConflict());
    }

    @Test
    void whenTheDonorCancelsTheItemReturnsToTheFeedAndRejectedClaimsReopen() throws Exception {
        donorAccepts(ashaClaim);

        mockMvc.perform(post(pickupUrl() + "/cancel").with(chitra()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(pickupUrl() + "/cancel").with(bilal()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(pickupUrl() + "/cancel").with(donor()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/donations?size=50").with(chitra()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem(donationId)));
        mockMvc.perform(get("/api/v1/donations/" + donationId + "/claims").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimantName").value("Asha"))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[1].claimantName").value("Bilal"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        // The private details are closed again.
        mockMvc.perform(get(pickupUrl()).with(asha()))
                .andExpect(status().isForbidden());

        // The donor picks someone else, then cancels again: the first cancelled claim stays closed.
        donorAccepts(bilalClaim);
        mockMvc.perform(get(pickupUrl()).with(bilal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiver.name").value("Bilal"));
        mockMvc.perform(post(pickupUrl() + "/cancel").with(donor()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/donations/" + donationId + "/claims").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[1].status").value("REJECTED"));
    }

    @Test
    void whenTheReceiverCancelsTheirClaimIsWithdrawnAndOthersReopen() throws Exception {
        donorAccepts(ashaClaim);

        mockMvc.perform(post(pickupUrl() + "/confirm").with(donor()))
                .andExpect(status().isOk());

        mockMvc.perform(post(pickupUrl() + "/cancel").with(asha()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/donations/" + donationId).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        mockMvc.perform(get("/api/v1/claims/mine").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("WITHDRAWN"));
        mockMvc.perform(get("/api/v1/claims/mine").with(bilal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("PENDING"));

        // The earlier confirmation was cleared: a new acceptance starts with nothing confirmed.
        donorAccepts(bilalClaim);
        mockMvc.perform(get(pickupUrl()).with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorConfirmed").value(false))
                .andExpect(jsonPath("$.receiverConfirmed").value(false));
    }

    @Test
    void pickupNeedsALoginAndAnExistingDonation() throws Exception {
        mockMvc.perform(post(pickupUrl() + "/confirm"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(pickupUrl() + "/cancel"))
                .andExpect(status().isUnauthorized());

        String unknown = "/api/v1/donations/" + UUID.randomUUID() + "/pickup";
        mockMvc.perform(get(unknown).with(donor()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(unknown + "/confirm").with(donor()))
                .andExpect(status().isNotFound());
    }
}
