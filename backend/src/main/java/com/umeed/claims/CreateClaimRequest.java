package com.umeed.claims;

import jakarta.validation.constraints.Size;

record CreateClaimRequest(
        @Size(max = 300, message = "Message must be at most 300 characters")
        String message) {
}
