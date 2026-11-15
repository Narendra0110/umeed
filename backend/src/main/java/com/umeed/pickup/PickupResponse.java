package com.umeed.pickup;

import java.util.UUID;

/**
 * The only response in the API that contains a pickup address or another person's phone number.
 * It is returned only to the donor and to the receiver whose claim was accepted.
 */
public record PickupResponse(
        UUID donationId,
        String donationTitle,
        String status,
        String pickupAddress,
        Party donor,
        Party receiver,
        boolean donorConfirmed,
        boolean receiverConfirmed) {

    public record Party(String name, String phoneNumber) {
    }
}
