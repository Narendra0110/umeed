package com.umeed.notifications;

/** Delivers one notification to one phone. */
interface PushSender {

    enum Result { SENT, INVALID_TOKEN, FAILED }

    Result send(String token, Notice notice);
}
