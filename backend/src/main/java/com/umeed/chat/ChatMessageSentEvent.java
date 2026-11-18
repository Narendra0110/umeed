package com.umeed.chat;

import java.util.UUID;

/** A chat message was sent. Carries no message text: notifications must not include it. */
public record ChatMessageSentEvent(
        UUID claimId, UUID donationId, String donationTitle, UUID senderId, String senderName, UUID recipientId) {
}
