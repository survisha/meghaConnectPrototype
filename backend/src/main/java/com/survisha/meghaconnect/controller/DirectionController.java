package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Direction;
import com.survisha.meghaconnect.repository.DirectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/directions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DirectionController {

    private final DirectionRepository directionRepository;

    @GetMapping
    public ResponseEntity<List<Direction>> getAll() {
        return ResponseEntity.ok(directionRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Direction> getById(@PathVariable Long id) {
        return directionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
