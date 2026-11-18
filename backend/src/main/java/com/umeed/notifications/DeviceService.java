package com.umeed.notifications;

import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserService userService;

    DeviceService(DeviceTokenRepository deviceTokenRepository, UserService userService) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userService = userService;
    }

    /**
     * Records that the caller is logged in on this phone. If the token belonged to someone else
     * (another person logged in on the same phone before), it moves to the caller.
     */
    @Transactional
    public void register(String firebaseUid, String token, String platform) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        DeviceToken.Platform parsed = platform == null ? DeviceToken.Platform.ANDROID : DeviceToken.Platform.valueOf(platform);
        String cleaned = token.strip();
        var existing = deviceTokenRepository.findByToken(cleaned);
        if (existing.isPresent()) {
            existing.get().assignTo(caller.id(), parsed);
            deviceTokenRepository.flush();
            return;
        }
        // Two registrations of the same new token at the same moment fail on the unique constraint;
        // the app registers again on its next start, so that rare case needs no special handling.
        deviceTokenRepository.saveAndFlush(DeviceToken.of(caller.id(), cleaned, parsed));
    }

    /**
     * The caller logs out on this phone. Only removes the token if it is the caller's.
     */
    @Transactional
    public void unregister(String firebaseUid, String token) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        deviceTokenRepository.deleteByTokenAndUserId(token.strip(), caller.id());
    }

    /**
     * For account deletion: forgets every phone of this user.
     */
    @Transactional
    public void removeAllFor(UUID userId) {
        deviceTokenRepository.deleteAllByUserId(userId);
    }
}
