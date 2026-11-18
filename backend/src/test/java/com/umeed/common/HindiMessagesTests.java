package com.umeed.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The guard for F07B: every message the backend can send to the app must have a Hindi version in
 * i18n/messages-hi.txt. It reads the source code, so a new English message without Hindi fails the build.
 * Plain unit test: no Spring, no database.
 */
class HindiMessagesTests {

    /** Messages written in one piece: error reasons, validation messages and the small helpers that build them. */
    private static final List<Pattern> MESSAGE_PATTERNS = List.of(
            Pattern.compile("ResponseStatusException\\(\\s*HttpStatus\\.\\w+,\\s*\"([^\"]+)\"\\s*\\)"),
            Pattern.compile("message\\s*=\\s*\"([^\"]+)\""),
            Pattern.compile("\\brespond\\([^,]+,\\s*\"([^\"]+)\""),
            Pattern.compile("\\bbadRequest\\(\\s*\"([^\"]+)\"\\s*\\)"),
            Pattern.compile("\\brequireCompleteProfile\\(\\s*\\w+,\\s*\"([^\"]+)\"\\s*\\)"),
            Pattern.compile("isBindingFailure\\(\\)\\s*\\?\\s*\"([^\"]+)\""));

    /** A message glued together from pieces cannot be looked up; this one is only ever seen by admins. */
    private static final Pattern GLUED = Pattern.compile("ResponseStatusException\\(\\s*HttpStatus\\.\\w+,\\s*\"[^\"]*\"\\s*\\+");
    private static final Set<String> GLUED_ALLOWED = Set.of("DonationService.java");

    @Test
    void everyMessageTheAppCanSeeHasAHindiVersion() throws IOException {
        Set<String> missing = new TreeSet<>();
        Set<String> found = new TreeSet<>();
        for (Path file : appFacingSources()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            for (Pattern pattern : MESSAGE_PATTERNS) {
                Matcher matcher = pattern.matcher(source);
                while (matcher.find()) {
                    String message = matcher.group(1);
                    found.add(message);
                    if (!HindiMessages.all().containsKey(message)) {
                        missing.add(file.getFileName() + ": " + message);
                    }
                }
            }
            if (GLUED.matcher(source).find() && !GLUED_ALLOWED.contains(file.getFileName().toString())) {
                missing.add(file.getFileName() + ": a message is glued together; write it as one sentence");
            }
        }
        assertTrue(found.size() > 80, "the scan found only " + found.size() + " messages; the patterns may be broken");
        assertEquals(Set.of(), missing, "add these to src/main/resources/i18n/messages-hi.txt");
    }

    @Test
    void theSuspensionMessageMatchesWhatTheAppRecognises() {
        // apps/mobile/src/lib/api.ts ACCOUNT_NOT_ACTIVE lists exactly this Hindi text.
        assertEquals("खाता सक्रिय नहीं है", HindiMessages.translate("Account is not active"));
    }

    @Test
    void unknownMessagesStayAsTheyAre() {
        assertEquals("Something new", HindiMessages.translate("Something new"));
        assertEquals(null, HindiMessages.translate(null));
    }

    @Test
    void theFileFormatIsChecked() {
        assertEquals("नमस्ते", HindiMessages.parse("# note\n\nHello => नमस्ते\n").get("Hello"));
        assertThrows(IllegalStateException.class, () -> HindiMessages.parse("Hello नमस्ते"));
        assertThrows(IllegalStateException.class, () -> HindiMessages.parse("Hello => a\nHello => b"));
        for (String hindi : HindiMessages.all().values()) {
            assertFalse(hindi.isBlank());
        }
    }

    /** All main sources except the admin module, whose dashboard stays in English. */
    private static List<Path> appFacingSources() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "umeed");
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(root.resolve("admin")))
                    .forEach(files::add);
        }
        return files;
    }
}
