package com.umeed.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The production settings (H02). Plain unit test: no Spring, no database. */
class ProductionChecksTests {

    @Test
    void allIsWellWithAReadableKeyAndAWritablePhotosFolder(@TempDir Path dir) throws IOException {
        Path key = Files.writeString(dir.resolve("key.json"), "{}");
        assertEquals(List.of(), ProductionChecks.problems(key.toString(), dir.toString()));
    }

    @Test
    void eachMissingPieceIsNamedClearly(@TempDir Path dir) {
        List<String> problems = ProductionChecks.problems("", dir.resolve("missing").toString());
        assertEquals(2, problems.size());
        assertTrue(problems.get(0).contains("UMEED_FIREBASE_CREDENTIALS"));
        assertTrue(problems.get(1).contains("photos folder"));

        problems = ProductionChecks.problems(dir.resolve("nope.json").toString(), "");
        assertTrue(problems.get(0).contains("cannot be read"));
        assertTrue(problems.get(1).contains("UMEED_PHOTOS_DIR"));
    }

    @Test
    void productionGivesSecretsNoDefaultAndListensOnlyLocally() throws IOException {
        Properties prod = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.properties")) {
            prod.load(in);
        }
        // "${NAME}" without ":default" means the backend cannot start until the server sets it.
        assertEquals("${UMEED_DB_URL}", prod.getProperty("spring.datasource.url"));
        assertEquals("${UMEED_DB_USERNAME}", prod.getProperty("spring.datasource.username"));
        assertEquals("${UMEED_DB_PASSWORD}", prod.getProperty("spring.datasource.password"));
        assertEquals("${UMEED_FIREBASE_CREDENTIALS}", prod.getProperty("umeed.notifications.credentials-file"));
        assertEquals("127.0.0.1", prod.getProperty("server.address"));
        assertEquals("true", prod.getProperty("spring.flyway.clean-disabled"));
        assertEquals("graceful", prod.getProperty("server.shutdown"));
    }
}
