package com.umeed.notifications;

import com.umeed.chat.ChatMessageSentEvent;
import com.umeed.claims.ClaimAcceptedEvent;
import com.umeed.claims.ClaimSubmittedEvent;
import com.umeed.needs.NeedOfferAcceptedEvent;
import com.umeed.needs.NeedOfferMadeEvent;
import com.umeed.pickup.HandoverConfirmedEvent;
import com.umeed.pickup.PickupCancelledEvent;
import com.umeed.users.BlockedUsers;
import com.umeed.users.UserLanguages;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns what happened into notifications, in each receiver's language (English or Hindi, F07B).
 * The texts never contain a phone number or an address. Blocked users never notify each other.
 * Names and item titles are shown as people typed them.
 */
@Component
class NotificationListener {

    static final String SCREEN_ITEM = "item";
    static final String SCREEN_CLAIMS = "claims";
    static final String SCREEN_PICKUP = "pickup";
    static final String SCREEN_CHAT = "chat";
    static final String SCREEN_NEED = "need";

    private static final String HINDI = "hi";

    private final PushDispatcher pushDispatcher;
    private final BlockedUsers blockedUsers;
    private final UserLanguages userLanguages;

    NotificationListener(PushDispatcher pushDispatcher, BlockedUsers blockedUsers, UserLanguages userLanguages) {
        this.pushDispatcher = pushDispatcher;
        this.blockedUsers = blockedUsers;
        this.userLanguages = userLanguages;
    }

    /** One notification before its language is known: the same text in English and in Hindi. */
    private record Draft(UUID userId, Text title, Text body, Map<String, String> data) {
    }

    private record Text(String en, String hi) {
        String in(String language) {
            return HINDI.equals(language) ? hi : en;
        }
    }

    @EventListener
    void onClaimSubmitted(ClaimSubmittedEvent event) {
        String title = quoted(event.donationTitle());
        String name = nameOrDefault(event.claimantName());
        send(event.claimantId(), List.of(itemDraft(
                event.donorId(),
                new Text("New claim on " + title, title + " पर नया क्लेम"),
                new Text(name + " would like it. Open Umeed to choose who gets it.",
                        name + " इसे लेना चाहते हैं। किसे देना है, यह चुनने के लिए उम्मीद खोलें।"),
                event.donationId(),
                SCREEN_CLAIMS)));
    }

    @EventListener
    void onClaimAccepted(ClaimAcceptedEvent event) {
        String title = quoted(event.donationTitle());
        List<Draft> drafts = new ArrayList<>();
        if (event.notifyReceiver()) {
            drafts.add(itemDraft(
                    event.receiverId(),
                    new Text(title + " is yours!", title + " आपका हुआ!"),
                    new Text("Open Umeed for the pickup details.", "पिकअप की जानकारी के लिए उम्मीद खोलें।"),
                    event.donationId(),
                    SCREEN_PICKUP));
        }
        for (UUID notChosen : event.notChosenIds()) {
            drafts.add(itemDraft(
                    notChosen,
                    new Text(title + " went to someone else", title + " किसी और को दिया गया"),
                    new Text("Thank you for asking. There are more items waiting on Umeed.",
                            "पूछने के लिए धन्यवाद। उम्मीद पर और भी सामान आपका इंतज़ार कर रहा है।"),
                    event.donationId(),
                    SCREEN_ITEM));
        }
        send(event.donorId(), drafts);
    }

    @EventListener
    void onPickupCancelled(PickupCancelledEvent event) {
        String title = quoted(event.donationTitle());
        send(event.cancelledById(), List.of(itemDraft(
                event.otherPartyId(),
                new Text("Pickup cancelled", "पिकअप रद्द हुआ"),
                new Text("The pickup of " + title + " was cancelled.", title + " का पिकअप रद्द कर दिया गया।"),
                event.donationId(),
                SCREEN_ITEM)));
    }

    @EventListener
    void onHandoverConfirmed(HandoverConfirmedEvent event) {
        String title = quoted(event.donationTitle());
        String name = nameOrDefault(event.confirmedByName());
        send(event.confirmedById(), List.of(itemDraft(
                event.otherPartyId(),
                new Text(name + " confirmed the handover", name + " ने सामान सौंपने की पुष्टि की"),
                new Text("Please confirm " + title + " too.", "कृपया आप भी " + title + " की पुष्टि करें।"),
                event.donationId(),
                SCREEN_PICKUP)));
    }

    @EventListener
    void onNeedOfferMade(NeedOfferMadeEvent event) {
        String need = quoted(event.needTitle());
        String item = quoted(event.donationTitle());
        String name = nameOrDefault(event.donorName());
        send(event.donorId(), List.of(needDraft(
                event.requesterId(),
                new Text("New offer for " + need, need + " के लिए नया ऑफ़र"),
                new Text(name + " offered " + item + ". Open Umeed to choose.",
                        name + " ने " + item + " ऑफ़र किया। चुनने के लिए उम्मीद खोलें।"),
                event.needId())));
    }

    @EventListener
    void onNeedOfferAccepted(NeedOfferAcceptedEvent event) {
        String need = quoted(event.needTitle());
        String item = quoted(event.donationTitle());
        String name = nameOrDefault(event.requesterName());
        List<Draft> drafts = new ArrayList<>();
        drafts.add(itemDraft(
                event.donorId(),
                new Text(name + " chose your " + item, name + " ने आपका " + item + " चुना"),
                new Text("Open Umeed for the pickup details.", "पिकअप की जानकारी के लिए उम्मीद खोलें।"),
                event.donationId(),
                SCREEN_PICKUP));
        for (UUID declined : event.declinedDonorIds()) {
            drafts.add(needDraft(
                    declined,
                    new Text("Another offer was chosen for " + need, need + " के लिए कोई दूसरा ऑफ़र चुना गया"),
                    new Text("Thank you for offering. Your item is still listed for others.",
                            "ऑफ़र करने के लिए धन्यवाद। आपका सामान दूसरों के लिए अब भी लिस्ट में है।"),
                    event.needId()));
        }
        send(event.requesterId(), drafts);
    }

    /** The message text is never included: it may contain an address, and notifications pass through Google. */
    @EventListener
    void onChatMessageSent(ChatMessageSentEvent event) {
        String title = quoted(event.donationTitle());
        String name = nameOrDefault(event.senderName());
        send(event.senderId(), List.of(new Draft(
                event.recipientId(),
                new Text(name + " sent you a message", name + " ने आपको संदेश भेजा"),
                new Text("About " + title + ". Open Umeed to reply.", title + " के बारे में। जवाब देने के लिए उम्मीद खोलें।"),
                Map.of(
                        "donationId", event.donationId().toString(),
                        "claimId", event.claimId().toString(),
                        "screen", SCREEN_CHAT))));
    }

    /**
     * Leaves out anyone who has blocked the person who caused the notification, or was blocked by them,
     * then writes each notification in its receiver's language.
     */
    private void send(UUID actorId, List<Draft> drafts) {
        List<Draft> allowed = drafts.stream()
                .filter(draft -> !draft.userId().equals(actorId))
                .filter(draft -> !blockedUsers.isBlockedEitherWay(actorId, draft.userId()))
                .toList();
        Map<UUID, String> languages = allowed.isEmpty()
                ? Map.of()
                : userLanguages.languagesByIds(allowed.stream().map(Draft::userId).toList());
        List<Notice> notices = allowed.stream()
                .map(draft -> {
                    String language = languages.get(draft.userId());
                    return new Notice(draft.userId(), draft.title().in(language), draft.body().in(language), draft.data());
                })
                .toList();
        pushDispatcher.dispatchAfterCommit(notices);
    }

    private static Draft itemDraft(UUID userId, Text title, Text body, UUID donationId, String screen) {
        return new Draft(userId, title, body, Map.of("donationId", donationId.toString(), "screen", screen));
    }

    private static Draft needDraft(UUID userId, Text title, Text body, UUID needId) {
        return new Draft(userId, title, body, Map.of("needId", needId.toString(), "screen", SCREEN_NEED));
    }

    private static String quoted(String title) {
        return "“" + title + "”";
    }

    /** A person without a name; "Umeed user" reads the same in both languages' notifications. */
    private static String nameOrDefault(String name) {
        return name == null || name.isBlank() ? "Umeed user" : name;
    }
}
