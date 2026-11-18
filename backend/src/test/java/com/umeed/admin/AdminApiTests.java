package com.umeed.admin;

import com.jayway.jsonpath.JsonPath;
import com.umeed.notifications.PushDispatcher;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * The admin is made by setting the role directly in the database, the same way the project owner does it.
 */
@SpringBootTest(properties = "umeed.storage.local.root=${java.io.tmpdir}/umeed-test-storage")
@Transactional
class AdminApiTests {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};
    private static final String NOTE = "{\"note\":\"Breaks the rules: asked for money\"}";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PushDispatcher pushDispatcher;

    private MockMvc mockMvc;
    private String categoryId;
    private String adminId;
    private String devId;
    private String ashaId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String categories = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        categoryId = JsonPath.read(categories, "$[0].id");
        adminId = register(admin(), "Admin Test");
        devId = register(dev(), "Dev Admintest");
        ashaId = register(asha(), "Asha Admintest");
        register(bilal(), "Bilal Admintest");
        // Plain SQL bypasses Hibernate, so save its pending changes first and forget its cached rows afterwards.
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE id = ?::uuid", adminId);
        entityManager.clear();
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

    private static RequestPostProcessor admin() {
        return user("test-uid-admin-admin", "+919000000101");
    }

    /** Donates and asks. */
    private static RequestPostProcessor dev() {
        return user("test-uid-admin-dev", "+919000000102");
    }

    /** Claims and reports. */
    private static RequestPostProcessor asha() {
        return user("test-uid-admin-asha", "+919000000103");
    }

    /** Reports too. */
    private static RequestPostProcessor bilal() {
        return user("test-uid-admin-bilal", "+919000000104");
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
        String body = mockMvc.perform(form.with(token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String postNeed(RequestPostProcessor token, String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/needs").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","title":"%s","description":"Needed for school, any colour.",
                                 "city":"Test city","area":"Test area","latitude":22.75,"longitude":75.89}
                                """.formatted(categoryId, title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String report(String path, RequestPostProcessor token, String reason) throws Exception {
        String body = mockMvc.perform(post(path).with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + reason + "\",\"details\":\"Asked me for money\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions send(MockHttpServletRequestBuilder request, RequestPostProcessor token, String body)
            throws Exception {
        return mockMvc.perform(request.with(token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> feedIds(RequestPostProcessor token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/donations").param("size", "50").with(token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    private List<String> needIds(RequestPostProcessor token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/needs").param("size", "50").with(token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    @Test
    void onlyAnAdminCanUseTheAdminApi() throws Exception {
        String anyId = UUID.randomUUID().toString();
        List<MockHttpServletRequestBuilder> reads = List.of(
                get("/api/v1/admin/me"),
                get("/api/v1/admin/stats"),
                get("/api/v1/admin/reports"),
                get("/api/v1/admin/users"),
                get("/api/v1/admin/users/" + adminId),
                get("/api/v1/admin/donations/" + anyId),
                get("/api/v1/admin/needs/" + anyId),
                get("/api/v1/admin/actions"));
        for (MockHttpServletRequestBuilder read : reads) {
            mockMvc.perform(read.with(dev()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Admins only"));
        }
        send(post("/api/v1/admin/users/" + ashaId + "/suspend"), dev(), NOTE).andExpect(status().isForbidden());
        send(post("/api/v1/admin/reports/" + anyId + "/review"), dev(), "{\"decision\":\"DISMISSED\"}")
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/stats")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/me").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void anAdminReviewsOrDismissesReportsOnce() throws Exception {
        String donationId = postItem(dev(), "Admin test jacket");
        String reportId = report("/api/v1/donations/" + donationId + "/reports", asha(), "SELLING_OR_ASKING_MONEY");
        report("/api/v1/donations/" + donationId + "/reports", bilal(), "FAKE_OR_MISLEADING");

        mockMvc.perform(get("/api/v1/admin/stats").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openReports").isNumber())
                .andExpect(jsonPath("$.availableListings").isNumber());

        String open = mockMvc.perform(get("/api/v1/admin/reports").param("size", "50").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> openIds = JsonPath.read(open, "$.items[*].id");
        assertTrue(openIds.contains(reportId));
        List<Object> mine = JsonPath.read(open, "$.items[?(@.id == '" + reportId + "')]");
        assertEquals("Admin test jacket", JsonPath.read(mine.get(0), "$.targetTitle"));
        assertEquals("Dev Admintest", JsonPath.read(mine.get(0), "$.ownerName"));
        assertEquals("Asha Admintest", JsonPath.read(mine.get(0), "$.reporterName"));
        assertEquals(2, ((Number) JsonPath.read(mine.get(0), "$.reportsOnTarget")).intValue());

        send(post("/api/v1/admin/reports/" + reportId + "/review"), admin(),
                "{\"decision\":\"DISMISSED\",\"note\":\"Looks fine\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.reviewNote").value("Looks fine"));
        send(post("/api/v1/admin/reports/" + reportId + "/review"), admin(), "{\"decision\":\"REVIEWED\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This report has already been handled"));
        send(post("/api/v1/admin/reports/" + reportId + "/review"), admin(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("decision"));
        send(post("/api/v1/admin/reports/" + UUID.randomUUID() + "/review"), admin(), "{\"decision\":\"REVIEWED\"}")
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/reports").param("status", "DISMISSED").param("size", "50").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem(reportId)));
        mockMvc.perform(get("/api/v1/admin/reports").param("status", "LOST").with(admin()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/actions").param("targetId", reportId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("DISMISS_REPORT"))
                .andExpect(jsonPath("$.items[0].adminName").value("Admin Test"))
                .andExpect(jsonPath("$.items[0].note").value("Looks fine"));
    }

    @Test
    void removingAListingClosesItsClaimsAndResolvesItsReports() throws Exception {
        String donationId = postItem(dev(), "Admin test bicycle");
        mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(bilal())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        String reportId = report("/api/v1/donations/" + donationId + "/reports", asha(), "PROHIBITED_ITEM");

        send(post("/api/v1/admin/donations/" + donationId + "/remove"), admin(), "{\"note\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("note"));

        send(post("/api/v1/admin/donations/" + donationId + "/remove"), admin(), NOTE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donation.status").value("REMOVED"))
                .andExpect(jsonPath("$.reports[0].id").value(reportId))
                .andExpect(jsonPath("$.reports[0].status").value("REVIEWED"))
                .andExpect(jsonPath("$.reports[0].reviewNote").value("Breaks the rules: asked for money"));

        assertEquals("REJECTED", jdbcTemplate.queryForObject(
                "SELECT status FROM claims WHERE donation_id = ?::uuid", String.class, donationId));
        assertFalse(feedIds(asha()).contains(donationId));

        // The admin still sees it; nobody else does.
        mockMvc.perform(get("/api/v1/admin/donations/" + donationId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donation.title").value("Admin test bicycle"));
        mockMvc.perform(get("/api/v1/donations/" + donationId).with(asha()))
                .andExpect(status().isNotFound());

        send(post("/api/v1/admin/donations/" + donationId + "/remove"), admin(), NOTE)
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/actions").param("targetId", donationId).with(admin()))
                .andExpect(jsonPath("$.items[0].action").value("REMOVE_DONATION"));
    }

    @Test
    void aReservedListingCannotBeRemovedByAnAdmin() throws Exception {
        String donationId = postItem(dev(), "Admin test lamp");
        String claim = mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(bilal())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/v1/claims/" + JsonPath.read(claim, "$.id") + "/accept").with(dev()))
                .andExpect(status().isOk());

        send(post("/api/v1/admin/donations/" + donationId + "/remove"), admin(), NOTE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only an available listing can be removed. This one is RESERVED"));
    }

    @Test
    void closingANeedResolvesItsReports() throws Exception {
        String needId = postNeed(dev(), "Admin test school bag");
        String reportId = report("/api/v1/needs/" + needId + "/reports", asha(), "OFFENSIVE");

        mockMvc.perform(get("/api/v1/admin/needs/" + needId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.need.status").value("OPEN"))
                .andExpect(jsonPath("$.reports[0].id").value(reportId))
                .andExpect(jsonPath("$.reports[0].needId").value(needId))
                .andExpect(jsonPath("$.reports[0].targetTitle").value("Admin test school bag"));

        send(post("/api/v1/admin/needs/" + needId + "/close"), admin(), NOTE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.need.status").value("CLOSED"))
                .andExpect(jsonPath("$.reports[0].status").value("REVIEWED"));
        assertFalse(needIds(asha()).contains(needId));

        send(post("/api/v1/admin/needs/" + needId + "/close"), admin(), NOTE)
                .andExpect(status().isConflict());
        send(post("/api/v1/admin/needs/" + UUID.randomUUID() + "/close"), admin(), NOTE)
                .andExpect(status().isNotFound());
    }

    @Test
    void suspendingHidesEverythingOfTheirsUntilItIsLifted() throws Exception {
        String donationId = postItem(dev(), "Admin test kettle");
        String needId = postNeed(dev(), "Admin test raincoat");
        assertTrue(feedIds(asha()).contains(donationId));
        assertTrue(needIds(asha()).contains(needId));

        send(post("/api/v1/admin/users/" + devId + "/suspend"), admin(), NOTE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.counts.availableListings").value(1))
                .andExpect(jsonPath("$.counts.openNeeds").value(1))
                .andExpect(jsonPath("$.history[0].action").value("SUSPEND_USER"));

        assertFalse(feedIds(asha()).contains(donationId));
        assertFalse(needIds(asha()).contains(needId));
        mockMvc.perform(get("/api/v1/donations/search").param("q", "Admin test kettle").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", not(hasItem(donationId))));

        // The suspended person can still see their own status, and nothing else.
        mockMvc.perform(get("/api/v1/users/me").with(dev()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        mockMvc.perform(get("/api/v1/donations").with(dev()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is not active"));

        send(post("/api/v1/admin/users/" + devId + "/suspend"), admin(), NOTE)
                .andExpect(status().isConflict());

        send(post("/api/v1/admin/users/" + devId + "/unsuspend"), admin(), "{\"note\":\"Explained, will not repeat\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.history[0].action").value("UNSUSPEND_USER"))
                .andExpect(jsonPath("$.history[1].action").value("SUSPEND_USER"));
        assertTrue(feedIds(asha()).contains(donationId));
        assertTrue(needIds(asha()).contains(needId));

        send(post("/api/v1/admin/users/" + devId + "/unsuspend"), admin(), NOTE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This account is not suspended"));
    }

    @Test
    void anAdminCannotBeSuspended() throws Exception {
        send(post("/api/v1/admin/users/" + adminId + "/suspend"), admin(), NOTE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("An admin cannot be suspended"));
        send(post("/api/v1/admin/users/" + UUID.randomUUID() + "/suspend"), admin(), NOTE)
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCanBeFoundByNameOrPhone() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").param("q", "asha admintest").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem(ashaId)))
                .andExpect(jsonPath("$.items[*].id", not(hasItem(devId))));
        mockMvc.perform(get("/api/v1/admin/users").param("q", "9000000103").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].phoneNumber").value("+919000000103"));
        // "_" and "%" are matched literally, not as wildcards.
        mockMvc.perform(get("/api/v1/admin/users").param("q", "admin_est").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get("/api/v1/admin/users").param("status", "gone").with(admin()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/users/" + ashaId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.displayName").value("Asha Admintest"))
                .andExpect(jsonPath("$.counts.reportsMade").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }
}
