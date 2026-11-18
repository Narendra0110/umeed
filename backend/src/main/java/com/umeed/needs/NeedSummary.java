package com.umeed.needs;

import java.util.UUID;

/** The few facts about a need that other modules use. Contains nothing private. */
public record NeedSummary(UUID id, UUID requesterId, String title, String status) {

    static NeedSummary from(NeedRequest need) {
        return new NeedSummary(need.getId(), need.getRequesterId(), need.getTitle(), need.getStatus().name());
    }
}
