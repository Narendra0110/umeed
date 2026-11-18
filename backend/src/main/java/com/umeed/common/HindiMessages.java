package com.umeed.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Hindi versions of the backend's messages (F07B), read once from i18n/messages-hi.txt.
 * Each line there is "English => Hindi". A message without a Hindi version is returned unchanged.
 */
public final class HindiMessages {

    static final String RESOURCE = "/i18n/messages-hi.txt";
    private static final String SEPARATOR = " => ";
    private static final Map<String, String> MESSAGES = load();

    private HindiMessages() {
    }

    /** The Hindi version of an English message, or the message itself when there is none. */
    public static String translate(String english) {
        return english == null ? null : MESSAGES.getOrDefault(english, english);
    }

    /** Every English message that has a Hindi version, with that version. */
    public static Map<String, String> all() {
        return MESSAGES;
    }

    /** True when the request's first preferred language is Hindi (the app sends "hi" or "en"). */
    public static boolean wanted(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        return header != null && header.trim().toLowerCase(Locale.ROOT).startsWith("hi");
    }

    static Map<String, String> parse(String text) {
        Map<String, String> messages = new LinkedHashMap<>();
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int at = line.indexOf(SEPARATOR);
            if (at <= 0 || at + SEPARATOR.length() >= line.length()) {
                throw new IllegalStateException("Bad line in " + RESOURCE + ": " + line);
            }
            String english = line.substring(0, at).strip();
            String hindi = line.substring(at + SEPARATOR.length()).strip();
            if (messages.put(english, hindi) != null) {
                throw new IllegalStateException("Message listed twice in " + RESOURCE + ": " + english);
            }
        }
        return Collections.unmodifiableMap(messages);
    }

    private static Map<String, String> load() {
        try (InputStream in = HindiMessages.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(RESOURCE + " is missing");
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
