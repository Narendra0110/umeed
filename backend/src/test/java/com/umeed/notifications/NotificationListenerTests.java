package com.umeed.notifications;

import com.umeed.claims.ClaimAcceptedEvent;
import com.umeed.users.BlockedUsers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
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
        NotificationListener listener = new NotificationListener(dispatcher, blockedUsers);

        listener.onClaimAccepted(new ClaimAcceptedEvent(
                UUID.randomUUID(), "Lamp", donor, receiver, List.of(blockedClaimant, otherClaimant), true));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notice>> captor = ArgumentCaptor.forClass(List.class);
        verify(dispatcher).dispatchAfterCommit(captor.capture());
        List<UUID> recipients = captor.getValue().stream().map(Notice::userId).toList();
        assertEquals(List.of(receiver, otherClaimant), recipients);
    }
}
