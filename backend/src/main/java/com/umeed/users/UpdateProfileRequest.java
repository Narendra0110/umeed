package com.umeed.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateProfileRequest(
        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 80, message = "Display name must be between 2 and 80 characters")
        String displayName) {
}
