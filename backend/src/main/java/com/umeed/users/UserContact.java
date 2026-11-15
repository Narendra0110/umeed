package com.umeed.users;

import java.util.UUID;

/**
 * PRIVATE contact details of a user. Only the pickup module may use this,
 * and only to answer the donor or the accepted receiver of a donation.
 */
public record UserContact(UUID id, String name, String phoneNumber) {
}
