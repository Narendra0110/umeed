package com.umeed.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Sends notifications once the change that caused them is saved: a failed action never notifies anyone.
 * Sending happens in the background, so a slow Firebase never slows down a request.
 */
@Component
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;
    private final Executor executor;

    @Autowired
    PushDispatcher(DeviceTokenRepository deviceTokenRepository, PushSender pushSender) {
        this(deviceTokenRepository, pushSender, backgroundExecutor());
    }

    /** For tests: a chosen sender and executor. */
    PushDispatcher(DeviceTokenRepository deviceTokenRepository, PushSender pushSender, Executor executor) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushSender = pushSender;
        this.executor = executor;
    }

    private static Executor backgroundExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("push-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(8);
        return executor;
    }

    /** Queues the notifications to go out after the current transaction commits (at once if there is none). */
    public void dispatchAfterCommit(List<Notice> notices) {
        if (notices.isEmpty()) {
            return;
        }
        List<Notice> copy = List.copyOf(notices);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            executor.execute(() -> sendNow(copy));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executor.execute(() -> sendNow(copy));
            }
        });
    }

    /**
     * Sends each notice to every phone of its user. Tokens that Firebase no longer accepts are deleted.
     * Never throws: a notification problem must not affect anything else.
     */
    void sendNow(List<Notice> notices) {
        try {
            Set<UUID> userIds = new HashSet<>();
            for (Notice notice : notices) {
                userIds.add(notice.userId());
            }
            Map<UUID, List<String>> tokensByUser = new HashMap<>();
            for (DeviceToken device : deviceTokenRepository.findByUserIdIn(userIds)) {
                tokensByUser.computeIfAbsent(device.getUserId(), id -> new ArrayList<>()).add(device.getToken());
            }
            for (Notice notice : notices) {
                for (String token : tokensByUser.getOrDefault(notice.userId(), List.of())) {
                    if (pushSender.send(token, notice) == PushSender.Result.INVALID_TOKEN) {
                        deviceTokenRepository.deleteByToken(token);
                    }
                }
            }
        } catch (RuntimeException e) {
            log.warn("Sending notifications failed", e);
        }
    }
}
