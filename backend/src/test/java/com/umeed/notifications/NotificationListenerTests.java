package com.umeed.notifications;

import com.umeed.claims.ClaimAcceptedEvent;
import com.umeed.users.BlockedUsers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Plain unit test: no Spring, no database. */
class NotificationListenerTests {

    @Test
    void blockedUsersAreNeverNotified() {
        UUID donor = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID blockedClaimant = UUID.randomUUID();
        UUID otherClaimant = UUID.randomUUID();
        BlockedUsers blockedUsers = new BlockedUsers() {
            @Override
            public Set<UUID> blockedEitherWay(UUID userId) {
                return userId.equals(donor) ? Set.of(blockedClaimant) : Set.of();
            }

            @Override
            public boolean isBlockedEitherWay(UUID userId, UUID otherUserId) {
                return (userId.equals(donor) && otherUserId.equals(blockedClaimant))
                        || (userId.equals(blockedClaimant) && otherUserId.equals(donor));
            }
        };
        PushDispatcher dispatcher = mock(PushDispatcher.class);
        NotificationListener listener = new NotificationListener(dispatcher, blockedUsers, ids -> Map.of());

        listener.onClaimAccepted(new ClaimAcceptedEvent(
                UUID.randomUUID(), "Lamp", donor, receiver, List.of(blockedClaimant, otherClaimant), true));

        List<UUID> recipients = sent(dispatcher).stream().map(Notice::userId).toList();
        assertEquals(List.of(receiver, otherClaimant), recipients);
    }

    @Test
    void eachPersonIsNotifiedInTheirOwnLanguage() {
        UUID donor = UUID.randomUUID();
        UUID hindiReceiver = UUID.randomUUID();
        UUID englishClaimant = UUID.randomUUID();
        UUID unknownClaimant = UUID.randomUUID();
        BlockedUsers nobodyBlocked = new BlockedUsers() {
            @Override
            public Set<UUID> blockedEitherWay(UUID userId) {
                return Set.of();
            }

            @Override
            public boolean isBlockedEitherWay(UUID userId, UUID otherUserId) {
                return false;
            }
        };
        PushDispatcher dispatcher = mock(PushDispatcher.class);
        NotificationListener listener = new NotificationListener(
                dispatcher, nobodyBlocked, ids -> Map.of(hindiReceiver, "hi", englishClaimant, "en"));

        listener.onClaimAccepted(new ClaimAcceptedEvent(
                UUID.randomUUID(), "Lamp", donor, hindiReceiver, List.of(englishClaimant, unknownClaimant), true));

        List<Notice> notices = sent(dispatcher);
        assertEquals("“Lamp” आपका हुआ!", notices.get(0).title());
        assertEquals("पिकअप की जानकारी के लिए उम्मीद खोलें।", notices.get(0).body());
        assertEquals("“Lamp” went to someone else", notices.get(1).title());
        // Without a known language, English.
        assertEquals("“Lamp” went to someone else", notices.get(2).title());
    }

    private static List<Notice> sent(PushDispatcher dispatcher) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notice>> captor = ArgumentCaptor.forClass(List.class);
        verify(dispatcher).dispatchAfterCommit(captor.capture());
        return captor.getValue();
    }
}
