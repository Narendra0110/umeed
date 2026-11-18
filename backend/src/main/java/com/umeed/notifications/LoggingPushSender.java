package com.umeed.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Used when no Firebase key is configured (development and tests): writes the notification to the log. */
class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public Result send(String token, Notice notice) {
        log.info("Notification not sent (no Firebase key configured): user {}: {} / {}",
                notice.userId(), notice.title(), notice.body());
        return Result.SENT;
    }
}
