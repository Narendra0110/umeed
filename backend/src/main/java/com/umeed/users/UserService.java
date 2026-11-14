package com.umeed.users;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
