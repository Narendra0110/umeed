package com.umeed.chat;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class ChatApiTests {

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
        return user("test-uid-chat-donor", "+919000000071");
    }

    private static RequestPostProcessor asha() {
        return user("test-uid-chat-asha", "+919000000072");
    }

    private static RequestPostProcessor bilal() {
        return user("test-uid-chat-bilal", "+919000000073");
    }

    private static RequestPostProcessor user(String uid, String phone) {
        return jwt().jwt(token -> token.subject(uid).claim("phone_number", phone));
    }

    private void register(RequestPostProcessor token, String name) throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(token)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/users/me").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + name + "\"}"))
                .andExpect(status().isOk());
    }

    private String postItem(String title) throws Exception {
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
        String created = mockMvc.perform(form.with(donor()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private String claim(String donationId, RequestPostProcessor token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/donations/" + donationId + "/claims").with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Posts an item, Asha claims it and the donor accepts. Returns {donationId, claimId}. */
    private String[] reservedForAsha(String title) throws Exception {
        String item = postItem(title);
        String claimId = claim(item, asha());
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/accept").with(donor()))
                .andExpect(status().isOk());
        return new String[] {item, claimId};
    }

    private ResultActions send(String claimId, RequestPostProcessor token, String text) throws Exception {
        return mockMvc.perform(post("/api/v1/claims/" + claimId + "/messages").with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + text + "\"}"));
    }

    private String sendOk(String claimId, RequestPostProcessor token, String text) throws Exception {
        String body = send(claimId, token, text)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.body").value(text))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
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

    @Test
    void theDonorAndTheReceiverCanChatDuringThePickup() throws Exception {
        register(donor(), "Dev");
        register(asha(), "Asha");
        register(bilal(), "Bilal");
        String[] reserved = reservedForAsha("Lamp");
        String claimId = reserved[1];

        // The pickup details point to the conversation.
        mockMvc.perform(get("/api/v1/donations/" + reserved[0] + "/pickup").with(asha()))
                .andExpect(jsonPath("$.claimId").value(claimId));

        // Before anything is written, the conversation is listed for both, open, with no last message.
        mockMvc.perform(get("/api/v1/chats").with(donor()))
                .andExpect(jsonPath("$[?(@.claimId == '" + claimId + "')].otherPartyName").value("Asha"))
                .andExpect(jsonPath("$[?(@.claimId == '" + claimId + "')].open").value(true));

        dispatched();
        String first = sendOk(claimId, asha(), "Hello, when can I come?");
        List<Notice> notices = dispatched();
        assertEquals(1, notices.size());
        assertEquals("Asha sent you a message", notices.get(0).title());
        assertFalse(notices.get(0).body().contains("when can I come"), "message text in a notification");
        assertEquals("chat", notices.get(0).data().get("screen"));
        assertEquals(claimId, notices.get(0).data().get("claimId"));

        sendOk(claimId, donor(), "Tomorrow at 5");
        sendOk(claimId, asha(), "Great, thanks");

        // Both see the whole conversation, oldest first, with their own messages marked.
        mockMvc.perform(get("/api/v1/claims/" + claimId + "/messages").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherPartyName").value("Asha"))
                .andExpect(jsonPath("$.donationTitle").value("Lamp"))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.messages.length()").value(3))
                .andExpect(jsonPath("$.messages[0].body").value("Hello, when can I come?"))
                .andExpect(jsonPath("$.messages[0].mine").value(false))
                .andExpect(jsonPath("$.messages[1].mine").value(true));

        // "after" returns only the newer messages.
        mockMvc.perform(get("/api/v1/claims/" + claimId + "/messages").param("after", first).with(asha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].body").value("Tomorrow at 5"));

        // The donor has 2 unread messages from Asha until they read the chat.
        mockMvc.perform(get("/api/v1/chats").with(donor()))
                .andExpect(jsonPath("$[?(@.claimId == '" + claimId + "')].unreadCount").value(2))
                .andExpect(jsonPath("$[?(@.claimId == '" + claimId + "')].lastMessage").value("Great, thanks"))
                .andExpect(jsonPath("$[?(@.claimId == '" + claimId + "')].lastMessageMine").value(false));
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/messages/read").with(donor()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/chats").with(donor()))
                .andExpect(jsonPath("$[?(@.claimId == '" + claimId + "')].unreadCount").value(0));
        // Asha now sees her messages as read.
        mockMvc.perform(get("/api/v1/claims/" + claimId + "/messages").with(asha()))
                .andExpect(jsonPath("$.messages[0].read").value(true));

        // Nobody else can read or write, and Bilal does not see the conversation.
        mockMvc.perform(get("/api/v1/claims/" + claimId + "/messages").with(bilal()))
                .andExpect(status().isForbidden());
        send(claimId, bilal(), "Let me in").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/chats").with(bilal()))
                .andExpect(jsonPath("$[?(@.claimId == '" + claimId + "')]").isEmpty());
    }

    @Test
    void theChatClosesWhenThePickupIsFinishedOrCancelled() throws Exception {
        register(donor(), "Dev");
        register(asha(), "Asha");

        // Handed over: read-only.
        String[] given = reservedForAsha("Lamp");
        sendOk(given[1], asha(), "On my way");
        mockMvc.perform(post("/api/v1/donations/" + given[0] + "/pickup/confirm").with(donor()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/donations/" + given[0] + "/pickup/confirm").with(asha()))
                .andExpect(status().isOk());
        send(given[1], donor(), "Thanks!").andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/claims/" + given[1] + "/messages").with(donor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.messages.length()").value(1));

        // Cancelled with messages: still readable and listed, but closed.
        String[] cancelled = reservedForAsha("Chair");
        sendOk(cancelled[1], donor(), "Sorry, plans changed");
        mockMvc.perform(post("/api/v1/donations/" + cancelled[0] + "/pickup/cancel").with(donor()))
                .andExpect(status().isNoContent());
        send(cancelled[1], asha(), "Oh no").andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/chats").with(asha()))
                .andExpect(jsonPath("$[?(@.claimId == '" + cancelled[1] + "')].open").value(false));

        // Cancelled without messages: there is no conversation.
        String[] silent = reservedForAsha("Table");
        mockMvc.perform(post("/api/v1/donations/" + silent[0] + "/pickup/cancel").with(asha()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/claims/" + silent[1] + "/messages").with(asha()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/chats").with(asha()))
                .andExpect(jsonPath("$[?(@.claimId == '" + silent[1] + "')]").isEmpty());
    }

    @Test
    void messagesMustBeValidAndClaimsMustBeAccepted() throws Exception {
        register(donor(), "Dev");
        register(asha(), "Asha");
        String[] reserved = reservedForAsha("Lamp");

        send(reserved[1], asha(), "   ").andExpect(status().isBadRequest());
        send(reserved[1], asha(), "x".repeat(1001)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/claims/" + reserved[1] + "/messages")
                        .param("after", UUID.randomUUID().toString()).with(asha()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/claims/" + UUID.randomUUID() + "/messages").with(asha()))
                .andExpect(status().isNotFound());

        // A pending claim has no chat yet.
        String pendingItem = postItem("Books");
        String pendingClaim = claim(pendingItem, asha());
        send(pendingClaim, asha(), "Hello").andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/chats")).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingAnAccountRemovesItsMessages() throws Exception {
        register(donor(), "Dev");
        register(asha(), "Asha");
        String[] reserved = reservedForAsha("Lamp");
        sendOk(reserved[1], asha(), "Hello");
        sendOk(reserved[1], donor(), "Hi");
        // Deleting is refused during a pickup, so the pickup is cancelled first.
        mockMvc.perform(post("/api/v1/donations/" + reserved[0] + "/pickup/cancel").with(asha()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/users/me").with(asha()))
                .andExpect(status().isNoContent());

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from chat_messages where claim_id = ?::uuid", Integer.class, reserved[1]));
        mockMvc.perform(get("/api/v1/claims/" + reserved[1] + "/messages").with(donor()))
                .andExpect(jsonPath("$.otherPartyName").value("Umeed user"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].body").value("Hi"));
    }
}
