package com.umeed.needs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record CreateOfferRequest(
        @NotNull(message = "Choose one of your items")
        UUID donationId,

        @Size(max = 300, message = "The message must be at most 300 characters")
        String message) {
}
