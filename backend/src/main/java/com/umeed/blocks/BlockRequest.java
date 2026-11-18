package com.umeed.blocks;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record BlockRequest(@NotNull(message = "Choose who to block") UUID userId) {
}
