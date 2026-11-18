package com.umeed.blocks;

import java.time.Instant;
import java.util.UUID;

/** Someone the caller has blocked. Never contains a phone number. */
public record BlockedUserResponse(UUID userId, String name, Instant blockedAt) {
}
