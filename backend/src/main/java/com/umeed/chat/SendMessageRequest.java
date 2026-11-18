package com.umeed.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SendMessageRequest(
        @NotBlank(message = "Write a message")
        @Size(max = 1000, message = "A message can be at most 1000 characters")
        String body) {
}
