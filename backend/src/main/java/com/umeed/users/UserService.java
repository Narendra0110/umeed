package com.umeed.users;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService implements UserLanguages {

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
     * Sets the app language ("en" or "hi") of the user identified by a verified Firebase identity.
     * Allowed for suspended accounts too, so the suspension notice can be read in their language.
     */
    @Transactional
    public UserResponse updateLanguage(String firebaseUid, String language) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() == User.Status.DELETED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        user.changeLanguage(language);
        return UserResponse.from(user);
    }

    /** Each user's language ("en" or "hi"), for notifications. Unknown ids are left out. */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> languagesByIds(Collection<UUID> userIds) {
        Map<UUID, String> languages = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            languages.put(user.getId(), user.getLanguage());
        }
        return languages;
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
     * Returns the caller if they are an active admin; otherwise 403. The role is read from the database,
     * never from the token or the request.
     */
    @Transactional(readOnly = true)
    public UserResponse requireAdmin(String firebaseUid) {
        UserResponse caller = requireActiveUser(firebaseUid);
        if (!User.Role.ADMIN.name().equals(caller.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only");
        }
        return caller;
    }

    /** The ids of every suspended user. Their listings and needs are left out of everyone's lists. */
    @Transactional(readOnly = true)
    public Set<UUID> suspendedUserIds() {
        return new HashSet<>(userRepository.findIdsByStatus(User.Status.SUSPENDED));
    }

    /** For the admin module: one user, including private details. The caller must already be an admin. */
    @Transactional(readOnly = true)
    public UserResponse getForAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * For the admin module: suspends an active user. Admins and deleted accounts cannot be suspended.
     * The caller must already be an admin.
     */
    @Transactional
    public UserResponse suspend(UUID userId) {
        User user = lockForAdmin(userId);
        if (user.getRole() == User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An admin cannot be suspended");
        }
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an active account can be suspended");
        }
        user.suspend();
        userRepository.flush();
        return UserResponse.from(user);
    }

    /** For the admin module: lifts a suspension. The caller must already be an admin. */
    @Transactional
    public UserResponse unsuspend(UUID userId) {
        User user = lockForAdmin(userId);
        if (user.getStatus() != User.Status.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is not suspended");
        }
        user.unsuspend();
        userRepository.flush();
        return UserResponse.from(user);
    }

    private User lockForAdmin(UUID userId) {
        return userRepository.findForUpdateById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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
