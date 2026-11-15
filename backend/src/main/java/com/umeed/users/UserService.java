package com.umeed.users;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
