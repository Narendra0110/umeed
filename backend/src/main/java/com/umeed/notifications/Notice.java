package com.umeed.notifications;

import java.util.Map;
import java.util.UUID;

/**
 * One notification for one user. Never contains a phone number or an address.
 * {@code data} tells the app which screen to open when the notification is tapped.
 */
public record Notice(UUID userId, String title, String body, Map<String, String> data) {

    public Notice {
        data = Map.copyOf(data);
    }
}
