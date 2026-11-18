package com.umeed.notifications;

import com.umeed.chat.ChatMessageSentEvent;
import com.umeed.claims.ClaimAcceptedEvent;
import com.umeed.claims.ClaimSubmittedEvent;
import com.umeed.pickup.HandoverConfirmedEvent;
import com.umeed.pickup.PickupCancelledEvent;
import com.umeed.users.BlockedUsers;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns what happened into notifications. The texts never contain a phone number or an address.
 * Blocked users never notify each other.
 */
@Component
class NotificationListener {

    static final String SCREEN_ITEM = "item";
    static final String SCREEN_CLAIMS = "claims";
    static final String SCREEN_PICKUP = "pickup";
    static final String SCREEN_CHAT = "chat";

    private final PushDispatcher pushDispatcher;
    private final BlockedUsers blockedUsers;

    NotificationListener(PushDispatcher pushDispatcher, BlockedUsers blockedUsers) {
        this.pushDispatcher = pushDispatcher;
        this.blockedUsers = blockedUsers;
    }

    @EventListener
    void onClaimSubmitted(ClaimSubmittedEvent event) {
        send(event.claimantId(), List.of(notice(
                event.donorId(),
                "New claim on “" + event.donationTitle() + "”",
                nameOrDefault(event.claimantName()) + " would like it. Open Umeed to choose who gets it.",
                event.donationId(),
                SCREEN_CLAIMS)));
    }

    @EventListener
    void onClaimAccepted(ClaimAcceptedEvent event) {
        List<Notice> notices = new ArrayList<>();
        notices.add(notice(
                event.receiverId(),
                "“" + event.donationTitle() + "” is yours!",
                "Open Umeed for the pickup details.",
                event.donationId(),
                SCREEN_PICKUP));
        for (UUID notChosen : event.notChosenIds()) {
            notices.add(notice(
                    notChosen,
                    "“" + event.donationTitle() + "” went to someone else",
                    "Thank you for asking. There are more items waiting on Umeed.",
                    event.donationId(),
                    SCREEN_ITEM));
        }
        send(event.donorId(), notices);
    }

    @EventListener
    void onPickupCancelled(PickupCancelledEvent event) {
        send(event.cancelledById(), List.of(notice(
                event.otherPartyId(),
                "Pickup cancelled",
                "The pickup of “" + event.donationTitle() + "” was cancelled.",
                event.donationId(),
                SCREEN_ITEM)));
    }

    @EventListener
    void onHandoverConfirmed(HandoverConfirmedEvent event) {
        send(event.confirmedById(), List.of(notice(
                event.otherPartyId(),
                nameOrDefault(event.confirmedByName()) + " confirmed the handover",
                "Please confirm “" + event.donationTitle() + "” too.",
                event.donationId(),
                SCREEN_PICKUP)));
    }

    /** The message text is never included: it may contain an address, and notifications pass through Google. */
    @EventListener
    void onChatMessageSent(ChatMessageSentEvent event) {
        send(event.senderId(), List.of(new Notice(
                event.recipientId(),
                nameOrDefault(event.senderName()) + " sent you a message",
                "About “" + event.donationTitle() + "”. Open Umeed to reply.",
                Map.of(
                        "donationId", event.donationId().toString(),
                        "claimId", event.claimId().toString(),
                        "screen", SCREEN_CHAT))));
    }

    /** Leaves out anyone who has blocked the person who caused the notification, or was blocked by them. */
    private void send(UUID actorId, List<Notice> notices) {
        List<Notice> allowed = notices.stream()
                .filter(notice -> !notice.userId().equals(actorId))
                .filter(notice -> !blockedUsers.isBlockedEitherWay(actorId, notice.userId()))
                .toList();
        pushDispatcher.dispatchAfterCommit(allowed);
    }

    private static Notice notice(UUID userId, String title, String body, UUID donationId, String screen) {
        return new Notice(userId, title, body, Map.of("donationId", donationId.toString(), "screen", screen));
    }

    private static String nameOrDefault(String name) {
        return name == null || name.isBlank() ? "Umeed user" : name;
    }
}
