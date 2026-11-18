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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Search and "Not interested" (F05). Runs against the development database in a transaction that is
 * rolled back. Every test item carries a random word, so other data in the database cannot match.
 */
@SpringBootTest(properties = "umeed.storage.local.root=${java.io.tmpdir}/umeed-test-storage")
@Transactional
class ExploreApiTests {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3, 4};

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private String categoryA;
    private String categoryB;
    private String word;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String categories = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        categoryA = JsonPath.read(categories, "$[0].id");
        categoryB = JsonPath.read(categories, "$[1].id");
        word = "zq" + UUID.randomUUID().toString().substring(0, 8);
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
        return user("test-uid-explore-donor", "+919000000091");
    }

    private static RequestPostProcessor seeker() {
        return user("test-uid-explore-seeker", "+919000000092");
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

    private String postItem(String title, String category, String city, String lat, String lng) throws Exception {
        MockMultipartHttpServletRequestBuilder form = multipart("/api/v1/donations");
        form.param("categoryId", category)
                .param("title", title)
                .param("description", "Posted by an automated test, in good condition.")
                .param("itemCondition", "GOOD")
                .param("city", city)
                .param("area", "Test area")
                .param("latitude", lat)
                .param("longitude", lng)
                .param("pickupAddress", "Flat 12, Test Residency, Test area, Test city");
        form.file(new MockMultipartFile("photos", "a.png", "image/png", PNG));
        String created = mockMvc.perform(form.with(donor()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private List<String> search(String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/donations/search?size=50&" + query).with(seeker()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    private List<String> homeFeed() throws Exception {
        String body = mockMvc.perform(get("/api/v1/donations?size=50").with(seeker()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    @Test
    void searchMatchesWordsAndCombinesFilters() throws Exception {
        register(donor(), "Dev");
        register(seeker(), "Asha");
        String bag = postItem("Blue school bag " + word, categoryA, "Indore", "22.75", "75.89");
        String shoes = postItem("Red school shoes " + word, categoryB, "Delhi", "28.60", "77.20");

        assertEquals(List.of(shoes, bag), search("q=" + word));
        // Words in order, anything between them, any letter case.
        String twoWords = mockMvc.perform(get("/api/v1/donations/search").param("q", "BAG " + word).with(seeker()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(List.of(bag), JsonPath.read(twoWords, "$.items[*].id"));
        assertEquals(List.of(bag), search("q=" + word + "&categoryId=" + categoryA));
        assertEquals(List.of(shoes), search("q=" + word + "&city=delhi"));
        assertTrue(search("q=" + word + "&city=Mumbai").isEmpty());

        // Near Indore: nearest first, and the distance limit leaves Delhi out.
        assertEquals(List.of(bag, shoes), search("q=" + word + "&lat=22.72&lng=75.86"));
        assertEquals(List.of(bag), search("q=" + word + "&lat=22.72&lng=75.86&maxKm=50"));
        assertTrue(search("q=" + word + "&lat=22.72&lng=75.86&maxKm=50&categoryId=" + categoryB).isEmpty());
    }

    @Test
    void searchLeavesOutOwnClaimedAndBlockedItems() throws Exception {
        String donorId = register(donor(), "Dev");
        register(seeker(), "Asha");
        String claimed = postItem("Lamp " + word, categoryA, "Indore", "22.75", "75.89");
        String other = postItem("Chair " + word, categoryA, "Indore", "22.75", "75.89");
        mockMvc.perform(post("/api/v1/donations/" + claimed + "/claims").with(seeker())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());

        assertEquals(List.of(other), search("q=" + word));

        // The donor never finds their own items.
        String own = mockMvc.perform(get("/api/v1/donations/search?q=" + word).with(donor()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(((List<?>) JsonPath.read(own, "$.items")).isEmpty());

        mockMvc.perform(post("/api/v1/blocks").with(seeker())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + donorId + "\"}"))
                .andExpect(status().isNoContent());
        assertTrue(search("q=" + word).isEmpty());
    }

    @Test
    void notInterestedHidesAnItemFromHomeOnly() throws Exception {
        register(donor(), "Dev");
        register(seeker(), "Asha");
        String lamp = postItem("Lamp " + word, categoryA, "Indore", "22.75", "75.89");
        assertTrue(homeFeed().contains(lamp));

        mockMvc.perform(post("/api/v1/donations/" + lamp + "/skip").with(seeker()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/donations/" + lamp + "/skip").with(seeker()))
                .andExpect(status().isNoContent());
        assertFalse(homeFeed().contains(lamp), "hidden item still on Home");
        String nearest = mockMvc.perform(get("/api/v1/donations?size=50&lat=22.72&lng=75.86").with(seeker()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(((List<?>) JsonPath.read(nearest, "$.items[*].id")).contains(lamp));
        // A search still finds it.
        assertEquals(List.of(lamp), search("q=" + word));

        mockMvc.perform(post("/api/v1/donations/" + UUID.randomUUID() + "/skip").with(seeker()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/donations/skips").with(seeker()))
                .andExpect(status().isNoContent());
        assertTrue(homeFeed().contains(lamp));
    }

    @Test
    void searchChecksItsFilters() throws Exception {
        register(seeker(), "Asha");
        mockMvc.perform(get("/api/v1/donations/search?maxKm=10").with(seeker())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/donations/search?lat=22.7").with(seeker())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/donations/search?lat=22.7&lng=75.8&maxKm=0").with(seeker()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/donations/search?q=" + "x".repeat(101)).with(seeker()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/donations/search")).andExpect(status().isUnauthorized());
    }

    @Test
    void specialCharactersAreSearchedLiterally() {
        assertEquals("%", DonationSearch.likePattern(null));
        assertEquals("%", DonationSearch.likePattern("   "));
        assertEquals("%school%bag%", DonationSearch.likePattern(" school   bag "));
        assertEquals("%50\\%%off\\_now%", DonationSearch.likePattern("50% off_now"));
    }
}
