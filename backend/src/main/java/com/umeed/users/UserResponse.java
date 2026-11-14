package com.umeed.users;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String phoneNumber,
        String displayName,
        boolean profileComplete,
        String role,
        String status) {

    static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getDisplayName() != null && !user.getDisplayName().isBlank(),
                user.getRole().name(),
                user.getStatus().name());
    }
}
