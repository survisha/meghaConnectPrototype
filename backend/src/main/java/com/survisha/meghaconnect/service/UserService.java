package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Get full name by username
     */
    public String getFullNameByUsername(String username) {
        log.debug("Fetching full name for username: {}", username);
        return userRepository.findByUsername(username)
            .map(User::getFullName)
            .orElse(username);
    }
}
