package com.umeed.needs;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Posting a need. Same rules as posting an item, without photos, condition or address. */
record CreateNeedRequest(
        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
        String description,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "Area is required")
        @Size(max = 100, message = "Area must be at most 100 characters")
        String area,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        Double longitude) {
}
