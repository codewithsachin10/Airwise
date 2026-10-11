package com.airwise.service;

import com.airwise.entity.Role;
import com.airwise.entity.User;
import com.airwise.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public User registerUser(String username, String rawPassword, String fullName, String phone, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username/email already registered: " + username);
        }

        User user = new User(
                username,
                passwordEncoder.encode(rawPassword),
                fullName,
                phone,
                role != null ? role : Role.ROLE_PASSENGER
        );

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(User user, String fullName, String phone, String newPassword) {
        user.setFullName(fullName);
        user.setPhone(phone);
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        return userRepository.save(user);
    }
}
