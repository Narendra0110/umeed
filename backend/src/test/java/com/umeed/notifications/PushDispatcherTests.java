package com.umeed.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dispatcher with a recording sender and a same-thread executor. Database changes made here are
 * always rolled back.
 */
@SpringBootTest
class PushDispatcherTests {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Remembers what was sent; tokens starting with "dead-" are reported as no longer valid. */
    private static final class RecordingSender implements PushSender {
        final List<String> sentTo = new ArrayList<>();

        @Override
        public Result send(String token, Notice notice) {
            sentTo.add(token);
            return token.startsWith("dead-") ? Result.INVALID_TOKEN : Result.SENT;
        }
    }

    private static Notice noticeFor(UUID userId) {
        return new Notice(userId, "Title", "Body", Map.of("screen", "item"));
    }

    @Test
    void sendingIsQueuedOnlyAfterACommit() {
        List<Runnable> queued = new ArrayList<>();
        PushDispatcher dispatcher = new PushDispatcher(deviceTokenRepository, new RecordingSender(), queued::add);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // Rolled back: nothing is ever queued.
        transaction.executeWithoutResult(status -> {
            dispatcher.dispatchAfterCommit(List.of(noticeFor(UUID.randomUUID())));
            assertTrue(queued.isEmpty(), "queued before the commit");
            status.setRollbackOnly();
        });
        assertTrue(queued.isEmpty(), "queued although rolled back");

        // Committed (this transaction changes no data): queued once, after the commit.
        transaction.executeWithoutResult(status -> {
            dispatcher.dispatchAfterCommit(List.of(noticeFor(UUID.randomUUID())));
            assertTrue(queued.isEmpty(), "queued before the commit");
        });
        assertEquals(1, queued.size());
    }

    @Test
    void sendsToEveryPhoneOfTheUserAndForgetsDeadTokens() {
        RecordingSender sender = new RecordingSender();
        PushDispatcher dispatcher = new PushDispatcher(deviceTokenRepository, sender, Runnable::run);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            UUID userId = jdbcTemplate.queryForObject(
                    "insert into users (firebase_uid, phone_number, display_name) "
                            + "values ('test-uid-push-dispatch', '+919000000064', 'Asha') returning id",
                    UUID.class);
            deviceTokenRepository.saveAndFlush(DeviceToken.of(userId, "live-" + UUID.randomUUID(), DeviceToken.Platform.ANDROID));
            String dead = "dead-" + UUID.randomUUID();
            deviceTokenRepository.saveAndFlush(DeviceToken.of(userId, dead, DeviceToken.Platform.ANDROID));

            dispatcher.sendNow(List.of(noticeFor(userId), noticeFor(UUID.randomUUID())));

            assertEquals(2, sender.sentTo.size(), "both phones of the user, none for the user with no phone");
            assertEquals(0, jdbcTemplate.queryForObject(
                    "select count(*) from device_tokens where token = ?", Integer.class, dead));
            assertEquals(1, jdbcTemplate.queryForObject(
                    "select count(*) from device_tokens where user_id = ?", Integer.class, userId));
            status.setRollbackOnly();
        });
    }

    @Test
    void outsideATransactionItSendsAtOnce() {
        RecordingSender sender = new RecordingSender();
        PushDispatcher dispatcher = new PushDispatcher(deviceTokenRepository, sender, Runnable::run);
        // No phone is registered for this user, so nothing is delivered, but no error either.
        dispatcher.dispatchAfterCommit(List.of(noticeFor(UUID.randomUUID())));
        assertTrue(sender.sentTo.isEmpty());
    }
}
