package com.umeed.needs;

import com.jayway.jsonpath.JsonPath;
import com.umeed.notifications.Notice;
import com.umeed.notifications.PushDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
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
 * The notification dispatcher is mocked, so nothing is sent.
 */
@SpringBootTest(properties = "umeed.storage.local.root=${java.io.tmpdir}/umeed-test-storage")
@Transactional
class NeedApiTests {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PushDispatcher pushDispatcher;

    private MockMvc mockMvc;
    private String categoryId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String categories = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        categoryId = JsonPath.read(categories, "$[0].id");
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

    /** Asks for things. */
    private static RequestPostProcessor asha() {
        return user("test-uid-need-asha", "+919000000081");
    }

    /** A donor. */
    private static RequestPostProcessor dev() {
        return user("test-uid-need-dev", "+919000000082");
    }

    /** Another donor. */
    private static RequestPostProcessor esha() {
        return user("test-uid-need-esha", "+919000000083");
    }

    /** Claims items. */
    private static RequestPostProcessor bilal() {
        return user("test-uid-need-bilal", "+919000000084");
    }

    private static RequestPostProcessor user(String uid, String phone) {
        return jwt().jwt(token -> token.subject(uid).claim("phone_number", phone));
    }

    private String register(RequestPostProcessor token, String name) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").with(token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        if (name != null) {
            body = mockMvc.perform(put("/api/v1/users/me").with(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayName\":\"" + name + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }
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
        String created = mockMvc.perform(form.with(token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private ResultActions postNeed(RequestPostProcessor token, String title, String lat, String lng) throws Exception {
        return mockMvc.perform(post("/api/v1/needs").with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":"%s","title":"%s","description":"For my daughter, size 5, any colour.",
                         "city":"Test city","area":"Test area","latitude":%s,"longitude":%s}
                        """.formatted(categoryId, title, lat, lng)));
    }

    private String needOf(RequestPostProcessor token, String title) throws Exception {
        String body = postNeed(token, title, "22.75", "75.89")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions offer(String needId, RequestPostProcessor token, String donationId) throws Exception {
        return mockMvc.perform(post("/api/v1/needs/" + needId + "/offers").with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"donationId\":\"" + donationId + "\",\"message\":\"Hope it fits\"}"));
    }

    private String offerOk(String needId, RequestPostProcessor token, String donationId) throws Exception {
        String body = offer(needId, token, donationId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.donationStatus").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private List<String> openNeedIds(RequestPostProcessor token, String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/needs" + query).with(token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    @SuppressWarnings("unchecked")
    private List<Notice> dispatched() {
        ArgumentCaptor<List<Notice>> captor = ArgumentCaptor.forClass(List.class);
        verify(pushDispatcher, atLeast(0)).dispatchAfterCommit(captor.capture());
        List<Notice> all = new ArrayList<>();
        captor.getAllValues().forEach(all::addAll);
        clearInvocations(pushDispatcher);
        return all;
    }

    private String needStatus(String needId) {
        return jdbcTemplate.queryForObject("select status from need_requests where id = ?::uuid", String.class, needId);
    }

    private String offerStatus(String offerId) {
        return jdbcTemplate.queryForObject("select status from need_offers where id = ?::uuid", String.class, offerId);
    }

    @Test
    void needsArePostedAndListedForOthers() throws Exception {
        register(asha(), "Asha");
        register(dev(), "Dev");
        String indore = needOf(asha(), "School shoes");
        String delhi = postNeed(asha(), "Winter coat", "28.60", "77.20")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        delhi = JsonPath.read(delhi, "$.id");

        // Others see them, nearest first near Indore; the person who asked does not see their own.
        List<String> nearIndore = openNeedIds(dev(), "?lat=22.72&lng=75.86&size=50");
        assertTrue(nearIndore.indexOf(indore) >= 0 && nearIndore.indexOf(indore) < nearIndore.indexOf(delhi));
        assertTrue(openNeedIds(dev(), "?size=50").contains(indore));
        assertFalse(openNeedIds(asha(), "?size=50").contains(indore));

        // The offer count is only for the person who asked.
        mockMvc.perform(get("/api/v1/needs/" + indore).with(dev()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterName").value("Asha"))
                .andExpect(jsonPath("$.pendingOfferCount", nullValue()))
                .andExpect(jsonPath("$.latitude").value(22.75));
        mockMvc.perform(get("/api/v1/needs/mine").with(asha()))
                .andExpect(jsonPath("$.items[?(@.id == '" + indore + "')].pendingOfferCount", contains(0)));

        // Validation, unknown category and an incomplete profile.
        postNeed(asha(), "ab", "22.75", "75.89").andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/needs").with(asha()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","title":"Shoes","description":"For my daughter, size 5.",
                                 "city":"C","area":"A","latitude":22.7,"longitude":75.8}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        register(bilal(), null);
        postNeed(bilal(), "Books", "22.75", "75.89").andExpect(status().isForbidden());
    }

    @Test
    void donorsOfferOnlyTheirOwnAvailableItems() throws Exception {
        String ashaId = register(asha(), "Asha");
        register(dev(), "Dev");
        register(esha(), "Esha");
        register(bilal(), "Bilal");
        String need = needOf(asha(), "School shoes");
        String devShoes = postItem(dev(), "Kids shoes");
        String eshaBag = postItem(esha(), "School bag");
        dispatched();

        offerOk(need, dev(), devShoes);
        List<Notice> notices = dispatched();
        assertEquals(1, notices.size());
        assertEquals(UUID.fromString(ashaId), notices.get(0).userId());
        assertEquals("New offer for “School shoes”", notices.get(0).title());
        assertEquals("need", notices.get(0).data().get("screen"));
        assertEquals(need, notices.get(0).data().get("needId"));

        offer(need, dev(), devShoes).andExpect(status().isConflict());
        offer(need, dev(), eshaBag).andExpect(status().isForbidden());
        offer(need, asha(), devShoes).andExpect(status().isForbidden());

        // A reserved item cannot be offered.
        String claimBody = mockMvc.perform(post("/api/v1/donations/" + eshaBag + "/claims").with(bilal())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/v1/claims/" + JsonPath.read(claimBody, "$.id") + "/accept").with(esha()))
                .andExpect(status().isOk());
        offer(need, esha(), eshaBag).andExpect(status().isConflict());

        // Only the person who asked sees the offers; the donor sees their own.
        mockMvc.perform(get("/api/v1/needs/" + need + "/offers").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].donorName").value("Dev"))
                .andExpect(jsonPath("$[0].donationTitle").value("Kids shoes"))
                .andExpect(jsonPath("$[0].message").value("Hope it fits"))
                .andExpect(jsonPath("$[0].pickupAddress").doesNotExist());
        mockMvc.perform(get("/api/v1/needs/" + need + "/offers").with(dev()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/offers/mine").with(dev()))
                .andExpect(jsonPath("$.items[0].needTitle").value("School shoes"));
        mockMvc.perform(get("/api/v1/needs/mine").with(asha()))
                .andExpect(jsonPath("$.items[?(@.id == '" + need + "')].pendingOfferCount", contains(1)));
    }

    @Test
    void choosingAnOfferReservesTheItemLikeAnAcceptedClaim() throws Exception {
        String ashaId = register(asha(), "Asha");
        String devId = register(dev(), "Dev");
        String eshaId = register(esha(), "Esha");
        String bilalId = register(bilal(), "Bilal");
        String need = needOf(asha(), "School shoes");
        String devShoes = postItem(dev(), "Kids shoes");
        String eshaShoes = postItem(esha(), "Sandals");
        String devOffer = offerOk(need, dev(), devShoes);
        String eshaOffer = offerOk(need, esha(), eshaShoes);
        // Bilal had claimed Dev's shoes too.
        mockMvc.perform(post("/api/v1/donations/" + devShoes + "/claims").with(bilal())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        dispatched();

        // Only the person who asked can choose.
        mockMvc.perform(post("/api/v1/offers/" + devOffer + "/accept").with(dev()))
                .andExpect(status().isForbidden());

        String accepted = mockMvc.perform(post("/api/v1/offers/" + devOffer + "/accept").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donationId").value(devShoes))
                .andReturn().getResponse().getContentAsString();
        String claimId = JsonPath.read(accepted, "$.claimId");

        assertEquals("FULFILLED", needStatus(need));
        assertEquals("ACCEPTED", offerStatus(devOffer));
        assertEquals("DECLINED", offerStatus(eshaOffer));

        // The item is reserved for Asha, with the usual pickup details, chat and claims.
        mockMvc.perform(get("/api/v1/donations/" + devShoes + "/pickup").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.claimId").value(claimId))
                .andExpect(jsonPath("$.receiver.name").value("Asha"));
        mockMvc.perform(get("/api/v1/donations/" + devShoes + "/claims").with(dev()))
                .andExpect(jsonPath("$[?(@.claimantName == 'Asha')].status", contains("ACCEPTED")))
                .andExpect(jsonPath("$[?(@.claimantName == 'Bilal')].status", contains("REJECTED")));
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/messages").with(asha())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Thank you!\"}"))
                .andExpect(status().isCreated());

        // Dev is told to arrange the pickup, Esha and Bilal that someone else was chosen; Asha is not notified.
        List<Notice> notices = dispatched();
        List<UUID> recipients = notices.stream().map(Notice::userId).toList();
        assertTrue(recipients.contains(UUID.fromString(devId)));
        assertTrue(recipients.contains(UUID.fromString(eshaId)));
        assertTrue(recipients.contains(UUID.fromString(bilalId)));
        Notice toDev = notices.stream().filter(n -> n.userId().equals(UUID.fromString(devId))
                && "pickup".equals(n.data().get("screen"))).findFirst().orElseThrow();
        assertEquals("Asha chose your “Kids shoes”", toDev.title());
        assertFalse(notices.stream().anyMatch(n -> n.userId().equals(UUID.fromString(ashaId))
                && !"chat".equals(n.data().get("screen"))), "Asha told about her own choice");

        // Nothing more can be chosen.
        mockMvc.perform(post("/api/v1/offers/" + eshaOffer + "/accept").with(asha()))
                .andExpect(status().isConflict());
    }

    @Test
    void anOfferCannotBeChosenOnceTheItemIsTaken() throws Exception {
        register(asha(), "Asha");
        register(dev(), "Dev");
        register(bilal(), "Bilal");
        String need = needOf(asha(), "School shoes");
        String devShoes = postItem(dev(), "Kids shoes");
        String devOffer = offerOk(need, dev(), devShoes);

        String claimBody = mockMvc.perform(post("/api/v1/donations/" + devShoes + "/claims").with(bilal())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/v1/claims/" + JsonPath.read(claimBody, "$.id") + "/accept").with(dev()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/offers/" + devOffer + "/accept").with(asha()))
                .andExpect(status().isConflict());
        assertEquals("OPEN", needStatus(need));
        assertEquals("PENDING", offerStatus(devOffer));
        mockMvc.perform(get("/api/v1/needs/" + need + "/offers").with(asha()))
                .andExpect(jsonPath("$[0].donationStatus").value("RESERVED"));
    }

    @Test
    void needsCanBeClosedAndOffersWithdrawn() throws Exception {
        register(asha(), "Asha");
        register(dev(), "Dev");
        register(esha(), "Esha");
        String need = needOf(asha(), "School shoes");
        String devOffer = offerOk(need, dev(), postItem(dev(), "Kids shoes"));
        String eshaOffer = offerOk(need, esha(), postItem(esha(), "Sandals"));

        // Withdrawing: only by its donor, only while waiting.
        mockMvc.perform(post("/api/v1/offers/" + eshaOffer + "/withdraw").with(dev()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/offers/" + eshaOffer + "/withdraw").with(esha()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/offers/" + eshaOffer + "/withdraw").with(esha()))
                .andExpect(status().isConflict());

        // Closing: only by the person who asked; the waiting offers are declined.
        mockMvc.perform(post("/api/v1/needs/" + need + "/close").with(dev()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/needs/" + need + "/close").with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        assertEquals("DECLINED", offerStatus(devOffer));
        assertEquals("WITHDRAWN", offerStatus(eshaOffer));
        assertFalse(openNeedIds(dev(), "?size=50").contains(need));
        offer(need, dev(), postItem(dev(), "Boots")).andExpect(status().isConflict());
    }

    @Test
    void removingTheOfferedItemWithdrawsItsOffers() throws Exception {
        register(asha(), "Asha");
        register(dev(), "Dev");
        String need = needOf(asha(), "School shoes");
        String devShoes = postItem(dev(), "Kids shoes");
        String devOffer = offerOk(need, dev(), devShoes);

        mockMvc.perform(delete("/api/v1/donations/" + devShoes).with(dev()))
                .andExpect(status().isNoContent());
        assertEquals("WITHDRAWN", offerStatus(devOffer));
    }

    @Test
    void blockingHidesNeedsAndClosesOffers() throws Exception {
        register(asha(), "Asha");
        String devId = register(dev(), "Dev");
        String need = needOf(asha(), "School shoes");
        String devOffer = offerOk(need, dev(), postItem(dev(), "Kids shoes"));

        mockMvc.perform(post("/api/v1/blocks").with(asha())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + devId + "\"}"))
                .andExpect(status().isNoContent());

        assertEquals("DECLINED", offerStatus(devOffer));
        assertFalse(openNeedIds(dev(), "?size=50").contains(need));
        assertFalse(openNeedIds(dev(), "?lat=22.72&lng=75.86&size=50").contains(need));
        mockMvc.perform(get("/api/v1/needs/" + need).with(dev())).andExpect(status().isNotFound());
        offer(need, dev(), postItem(dev(), "Boots")).andExpect(status().isForbidden());
    }

    @Test
    void deletingAnAccountClosesNeedsAndWithdrawsOffers() throws Exception {
        register(asha(), "Asha");
        register(dev(), "Dev");
        register(esha(), "Esha");
        String ashaNeed = needOf(asha(), "School shoes");
        String devNeed = needOf(dev(), "Study table");
        String devOffer = offerOk(ashaNeed, dev(), postItem(dev(), "Kids shoes"));
        String eshaOffer = offerOk(devNeed, esha(), postItem(esha(), "Table"));

        mockMvc.perform(delete("/api/v1/users/me").with(dev()))
                .andExpect(status().isNoContent());

        assertEquals("CLOSED", needStatus(devNeed));
        assertEquals("DECLINED", offerStatus(eshaOffer));
        assertEquals("WITHDRAWN", offerStatus(devOffer));
        assertEquals("OPEN", needStatus(ashaNeed));
    }
}
