package com.umeed.claims;

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
import static org.hamcrest.Matchers.not;
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
class ClaimApiTests {

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
        return user("test-uid-claim-donor", "+919000000011");
    }

    private static RequestPostProcessor asha() {
        return user("test-uid-claim-asha", "+919000000012");
    }

    private static RequestPostProcessor bilal() {
        return user("test-uid-claim-bilal", "+919000000013");
    }

    private static RequestPostProcessor chitra() {
        return user("test-uid-claim-chitra", "+919000000014");
    }

    private static RequestPostProcessor user(String uid, String phone) {
        return jwt().jwt(token -> token.subject(uid).claim("phone_number", phone));
    }

    /** Registers a user. A null name leaves the profile incomplete. */
    private void register(RequestPostProcessor token, String name) throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(token))
                .andExpect(status().isOk());
        if (name != null) {
            mockMvc.perform(put("/api/v1/users/me").with(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayName\":\"" + name + "\"}"))
                    .andExpect(status().isOk());
        }
    }

    /** Registers the donor and posts one donation. Returns the donation id. */
    private String donorPostsDonation() throws Exception {
        register(donor(), "Dev");
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

    /** Submits a claim and returns the claim id. */
    private String claim(String donationId, RequestPostProcessor token, String message) throws Exception {
        String body = mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.donationTitle").value("Winter jacket"))
                .andExpect(jsonPath("$.donationStatus").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void severalPeopleCanClaimUntilTheDonorAcceptsOne() throws Exception {
        String donationId = donorPostsDonation();
        register(asha(), "Asha");
        register(bilal(), "Bilal");
        register(chitra(), "Chitra");

        String ashaClaim = claim(donationId, asha(), "I need it for my brother");
        String bilalClaim = claim(donationId, bilal(), "Would help a lot");

        // The donor sees both claims, with names and messages but no phone numbers.
        mockMvc.perform(get("/api/v1/donations/" + donationId + "/claims").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].claimantName").value("Asha"))
                .andExpect(jsonPath("$[0].message").value("I need it for my brother"))
                .andExpect(jsonPath("$[1].claimantName").value("Bilal"))
                .andExpect(jsonPath("$[0].phoneNumber").doesNotExist());

        // A claimant cannot see who else claimed.
        mockMvc.perform(get("/api/v1/donations/" + donationId + "/claims").with(asha()))
                .andExpect(status().isForbidden());

        // Only the donor can accept.
        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/accept").with(bilal()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/accept").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.claimantName").value("Asha"))
                .andExpect(jsonPath("$.donationStatus").value("RESERVED"));

        // The item is reserved and has left the feed.
        mockMvc.perform(get("/api/v1/donations/" + donationId).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
        mockMvc.perform(get("/api/v1/donations?size=50").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", not(hasItem(donationId))));

        // The other claim was rejected, and its owner can see that.
        mockMvc.perform(get("/api/v1/donations/" + donationId + "/claims").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$[1].status").value("REJECTED"));
        mockMvc.perform(get("/api/v1/claims/mine").with(bilal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(bilalClaim))
                .andExpect(jsonPath("$.items[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.items[0].donationTitle").value("Winter jacket"))
                .andExpect(jsonPath("$.items[0].donationStatus").value("RESERVED"));

        // Nobody else can claim a reserved item, and a second acceptance is refused.
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(chitra()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This donation is no longer available"));
        mockMvc.perform(post("/api/v1/claims/" + bilalClaim + "/accept").with(donor()))
                .andExpect(status().isConflict());
    }

    @Test
    void claimRulesAreEnforced() throws Exception {
        String donationId = donorPostsDonation();
        register(asha(), "Asha");
        register(bilal(), null);

        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(donor()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot claim your own donation"));

        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(bilal()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Complete your profile before claiming a donation"));

        // A claim needs no message.
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(asha()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").doesNotExist());

        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(asha()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have already claimed this donation"));
    }

    @Test
    void invalidClaimRequestsAreRefused() throws Exception {
        String donationId = donorPostsDonation();
        register(asha(), "Asha");

        mockMvc.perform(post("/api/v1/donations/" + UUID.randomUUID() + "/claims").with(asha()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Donation not found"));

        String tooLong = "x".repeat(301);
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(asha())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("message"));

        mockMvc.perform(post("/api/v1/claims/" + UUID.randomUUID() + "/accept").with(asha()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Claim not found"));
    }

    @Test
    void aClaimantCanWithdrawTheirOwnPendingClaimOnce() throws Exception {
        String donationId = donorPostsDonation();
        register(asha(), "Asha");
        register(bilal(), "Bilal");
        String ashaClaim = claim(donationId, asha(), "Please");

        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/withdraw").with(bilal()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/withdraw").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/withdraw").with(asha()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only a pending claim can be withdrawn"));

        // Withdrawing is final for that item, and the item is still open to others.
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(asha()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(bilal()))
                .andExpect(status().isCreated());
    }

    @Test
    void removingAListingClosesItsOpenClaims() throws Exception {
        String donationId = donorPostsDonation();
        register(asha(), "Asha");
        String ashaClaim = claim(donationId, asha(), "Please");

        mockMvc.perform(delete("/api/v1/donations/" + donationId).with(donor()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/claims/mine").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ashaClaim))
                .andExpect(jsonPath("$.items[0].status").value("REJECTED"));
    }

    @Test
    void claimsAreNotVisibleWithoutLogin() throws Exception {
        String anyId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/donations/" + anyId + "/claims"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/donations/" + anyId + "/claims"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/claims/" + anyId + "/accept"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/claims/" + anyId + "/withdraw"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/claims/mine"))
                .andExpect(status().isUnauthorized());
    }
}
