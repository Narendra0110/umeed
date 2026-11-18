package com.umeed.users;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

record UpdateLanguageRequest(
        @NotNull(message = "Language is required")
        @Pattern(regexp = "en|hi", message = "Language must be en or hi")
        String language) {
}
