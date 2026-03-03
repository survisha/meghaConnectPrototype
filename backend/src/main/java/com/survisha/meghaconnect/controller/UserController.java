package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HCM','SAIDUL_OSD')")
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HCM','SAIDUL_OSD')")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
