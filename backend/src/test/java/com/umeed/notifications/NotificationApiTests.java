package com.umeed.notifications;

import com.jayway.jsonpath.JsonPath;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against the development database in a transaction that is rolled back. The dispatcher is replaced
 * by a mock, so these tests check WHO is notified and WITH WHAT, without sending anything.
 */
@SpringBootTest(properties = "umeed.storage.local.root=${java.io.tmpdir}/umeed-test-storage")
@Transactional
class NotificationApiTests {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PushDispatcher pushDispatcher;

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
        return user("test-uid-push-donor", "+919000000061");
    }

    private static RequestPostProcessor asha() {
        return user("test-uid-push-asha", "+919000000062");
    }

    private static RequestPostProcessor bilal() {
        return user("test-uid-push-bilal", "+919000000063");
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

    /** Every notice dispatched since the last clear, in order. */
    @SuppressWarnings("unchecked")
    private List<Notice> dispatched() {
        ArgumentCaptor<List<Notice>> captor = ArgumentCaptor.forClass(List.class);
        verify(pushDispatcher, org.mockito.Mockito.atLeast(0)).dispatchAfterCommit(captor.capture());
        List<Notice> all = new ArrayList<>();
        captor.getAllValues().forEach(all::addAll);
        clearInvocations(pushDispatcher);
        return all;
    }

    private static void assertNoPrivateDetails(List<Notice> notices) {
        for (Notice notice : notices) {
            String text = notice.title() + " " + notice.body();
            assertFalse(text.contains("Flat 12"), "address in a notification");
            assertFalse(text.contains("+91"), "phone number in a notification");
        }
    }

    @Test
    void phonesAreRegisteredMovedAndUnregistered() throws Exception {
        String ashaId = register(asha(), "Asha");
        String bilalId = register(bilal(), "Bilal");
        String token = "test-device-token-" + UUID.randomUUID();
        String body = "{\"token\":\"" + token + "\"}";

        mockMvc.perform(put("/api/v1/devices").with(asha()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        // Registering again changes nothing.
        mockMvc.perform(put("/api/v1/devices").with(asha()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        assertEquals(ashaId, ownerOf(token));

        // Bilal logs in on the same phone: the token moves to him.
        mockMvc.perform(put("/api/v1/devices").with(bilal()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        assertEquals(bilalId, ownerOf(token));

        // Asha can no longer remove it; Bilal can.
        mockMvc.perform(post("/api/v1/devices/unregister").with(asha()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        assertEquals(bilalId, ownerOf(token));
        mockMvc.perform(post("/api/v1/devices/unregister").with(bilal()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from device_tokens where token = ?", Integer.class, token));

        // Bad requests.
        mockMvc.perform(put("/api/v1/devices").with(asha()).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/devices").with(asha()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"x\",\"platform\":\"WINDOWS\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/devices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletingAnAccountForgetsItsPhones() throws Exception {
        register(asha(), "Asha");
        String token = "test-device-token-" + UUID.randomUUID();
        mockMvc.perform(put("/api/v1/devices").with(asha()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/users/me").with(asha()))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from device_tokens where token = ?", Integer.class, token));
    }

    @Test
    void theRightPeopleAreToldAboutClaimsAndPickups() throws Exception {
        String donorId = register(donor(), "Dev");
        String ashaId = register(asha(), "Asha");
        String bilalId = register(bilal(), "Bilal");
        String item = postItem(donor(), "Lamp");
        dispatched();

        // A claim tells the donor, and opens the claims list when tapped.
        String ashaClaim = claim(item, asha());
        List<Notice> onClaim = dispatched();
        assertEquals(1, onClaim.size());
        assertEquals(UUID.fromString(donorId), onClaim.get(0).userId());
        assertEquals("New claim on “Lamp”", onClaim.get(0).title());
        assertTrue(onClaim.get(0).body().startsWith("Asha "));
        assertEquals("claims", onClaim.get(0).data().get("screen"));
        assertEquals(item, onClaim.get(0).data().get("donationId"));
        claim(item, bilal());
        dispatched();

        // Accepting tells the receiver (pickup screen) and everyone not chosen (item screen).
        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/accept").with(donor()))
                .andExpect(status().isOk());
        List<Notice> onAccept = dispatched();
        assertEquals(2, onAccept.size());
        Notice toAsha = onAccept.stream().filter(n -> n.userId().equals(UUID.fromString(ashaId))).findFirst().orElseThrow();
        Notice toBilal = onAccept.stream().filter(n -> n.userId().equals(UUID.fromString(bilalId))).findFirst().orElseThrow();
        assertEquals("“Lamp” is yours!", toAsha.title());
        assertEquals("pickup", toAsha.data().get("screen"));
        assertEquals("“Lamp” went to someone else", toBilal.title());
        assertEquals("item", toBilal.data().get("screen"));
        assertNoPrivateDetails(onAccept);

        // The first confirmation asks the other side to confirm too; confirming twice sends nothing more.
        mockMvc.perform(post("/api/v1/donations/" + item + "/pickup/confirm").with(donor()))
                .andExpect(status().isOk());
        List<Notice> onConfirm = dispatched();
        assertEquals(1, onConfirm.size());
        assertEquals(UUID.fromString(ashaId), onConfirm.get(0).userId());
        assertEquals("Dev confirmed the handover", onConfirm.get(0).title());
        mockMvc.perform(post("/api/v1/donations/" + item + "/pickup/confirm").with(donor()))
                .andExpect(status().isOk());
        assertTrue(dispatched().isEmpty());

        // The second side's confirmation completes the item and needs no notification.
        mockMvc.perform(post("/api/v1/donations/" + item + "/pickup/confirm").with(asha()))
                .andExpect(status().isOk());
        assertTrue(dispatched().isEmpty());
    }

    @Test
    void aCancelledPickupTellsTheOtherPerson() throws Exception {
        String donorId = register(donor(), "Dev");
        String ashaId = register(asha(), "Asha");
        String item = postItem(donor(), "Lamp");
        String ashaClaim = claim(item, asha());
        mockMvc.perform(post("/api/v1/claims/" + ashaClaim + "/accept").with(donor()))
                .andExpect(status().isOk());
        dispatched();

        mockMvc.perform(post("/api/v1/donations/" + item + "/pickup/cancel").with(asha()))
                .andExpect(status().isNoContent());
        List<Notice> onCancel = dispatched();
        assertEquals(1, onCancel.size());
        assertEquals(UUID.fromString(donorId), onCancel.get(0).userId());
        assertEquals("Pickup cancelled", onCancel.get(0).title());
        assertFalse(onCancel.stream().anyMatch(n -> n.userId().equals(UUID.fromString(ashaId))));
    }

    @Test
    void aRefusedActionNotifiesNobody() throws Exception {
        register(donor(), "Dev");
        String item = postItem(donor(), "Lamp");
        dispatched();
        mockMvc.perform(post("/api/v1/donations/" + item + "/claims").with(donor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(pushDispatcher, never()).dispatchAfterCommit(any());
    }

    private String ownerOf(String token) {
        return jdbcTemplate.queryForObject(
                "select user_id::text from device_tokens where token = ?", String.class, token);
    }
}
