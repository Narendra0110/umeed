package com.umeed.notifications;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A phone's Firebase notification token. Platform is ANDROID (the default) or IOS. */
record DeviceRequest(
        @NotBlank(message = "The device token is missing")
        @Size(max = 512, message = "The device token is too long")
        String token,
        @Pattern(regexp = "ANDROID|IOS", message = "Platform must be ANDROID or IOS")
        String platform) {
}
