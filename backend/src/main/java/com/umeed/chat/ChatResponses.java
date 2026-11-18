package com.umeed.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** What the chat endpoints answer. Never contains a phone number or an address. */
final class ChatResponses {

    private ChatResponses() {
    }

    record Message(UUID id, boolean mine, String body, Instant createdAt, boolean read) {

        static Message from(ChatMessage message, UUID callerId) {
            return new Message(
                    message.getId(),
                    message.getSenderId().equals(callerId),
                    message.getBody(),
                    message.getCreatedAt(),
                    message.getReadAt() != null);
        }
    }

    /** One conversation with its messages. {@code open} is false once sending is closed. */
    record Thread(
            UUID claimId,
            UUID donationId,
            String donationTitle,
            String otherPartyName,
            boolean open,
            List<Message> messages) {
    }

    /** One row of the Chats list. The last message fields are null when nothing has been written yet. */
    record Conversation(
            UUID claimId,
            UUID donationId,
            String donationTitle,
            String otherPartyName,
            boolean open,
            String lastMessage,
            boolean lastMessageMine,
            Instant lastMessageAt,
            long unreadCount) {
    }
}
