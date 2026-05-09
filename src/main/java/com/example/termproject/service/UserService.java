package com.example.termproject.service;

import com.example.termproject.model.User;
import com.example.termproject.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user, boolean isRenter, boolean isProvider) {
        // 1. Hash the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Save core user data
        User savedUser = userRepository.save(user);
        Long userId = savedUser.getUserId();

        // 3. Handle Renter Role
        if (isRenter) {
            userRepository.registerAsRenter(userId);
            userRepository.addRole(userId, "RENTER");
        }

        // 4. Handle Provider Role
        if (isProvider) {
            userRepository.registerAsProvider(userId);
            userRepository.addRole(userId, "PROVIDER");
        }

        return savedUser;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
    /*
     * Milestone 6 Design (Implementation not Required)
     * import org.slf4j.Logger;
     * import org.slf4j.LoggerFactory;
     * 
     * public class MyExample {
     * // Create a logger instance for this class
     * private static final Logger logger =
     * LoggerFactory.getLogger(MyExample.class);
     * 
     * public void processData(String data) {
     * // Use placeholders for performance
     * logger.info("Processing data: {}", data);
     * 
     * try {
     * // ... logic ...
     * } catch (Exception e) {
     * logger.error("An error occurred during processing", e);
     * }
     * }
     * }
     */
}
