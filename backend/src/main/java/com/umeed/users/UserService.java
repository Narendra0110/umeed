package com.umeed.users;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the user for a verified Firebase identity, registering them on first login.
     * Both arguments must come from a verified token, never from client input.
     */
    public UserResponse findOrRegister(String firebaseUid, String phoneNumber) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .map(UserResponse::from)
                .orElseGet(() -> register(firebaseUid, phoneNumber));
    }

    /**
     * Sets the display name of the user identified by a verified Firebase identity.
     */
    @Transactional
    public UserResponse updateDisplayName(String firebaseUid, String displayName) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        user.changeDisplayName(displayName);
        return UserResponse.from(user);
    }

    /**
     * Returns the active user for a verified Firebase identity. For use by other modules.
     */
    @Transactional(readOnly = true)
    public UserResponse requireActiveUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        return UserResponse.from(user);
    }

    /**
     * True when the user exists and their account is active.
     */
    @Transactional(readOnly = true)
    public boolean isActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getStatus() == User.Status.ACTIVE)
                .orElse(false);
    }

    /**
     * For account deletion: erases the user's identity (Firebase id, phone number and name).
     * The caller must already have removed or closed everything else that belongs to the user.
     */
    @Transactional
    public void markDeleted(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .markDeleted();
        userRepository.flush();
    }

    /**
     * Returns display names for the given user ids. Users without a name get a neutral placeholder.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> displayNamesByIds(Collection<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            String name = user.getDisplayName();
            names.put(user.getId(), name != null && !name.isBlank() ? name : "Umeed user");
        }
        return names;
    }

    /**
     * PRIVATE contact details, including the phone number. The caller must already have checked
     * that the person asking is allowed to see them.
     */
    @Transactional(readOnly = true)
    public UserContact contactOf(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String name = user.getDisplayName();
        return new UserContact(
                user.getId(), name != null && !name.isBlank() ? name : "Umeed user", user.getPhoneNumber());
    }

    private UserResponse register(String firebaseUid, String phoneNumber) {
        try {
            return UserResponse.from(userRepository.saveAndFlush(User.register(firebaseUid, phoneNumber)));
        } catch (DataIntegrityViolationException e) {
            // Another request registered this user at the same moment; return that one.
            return userRepository.findByFirebaseUid(firebaseUid)
                    .map(UserResponse::from)
                    .orElseThrow(() -> e);
        }
    }
}
