package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.service.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VisitorController {

    private final VisitorService visitorService;

    @GetMapping("/search/phone/{phone}")
    public ResponseEntity<Visitor> findByPhone(@PathVariable String phone) {
        return visitorService.findByPhone(phone)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/epic/{epic}")
    public ResponseEntity<Visitor> findByEpic(@PathVariable String epic) {
        return visitorService.findByEpic(epic)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Visitor>> searchByName(@RequestParam String q) {
        return ResponseEntity.ok(visitorService.searchByName(q));
    }

    @GetMapping("/search/district/{district}")
    public ResponseEntity<List<Visitor>> findByDistrict(@PathVariable String district) {
        return ResponseEntity.ok(visitorService.findByDistrict(district));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Visitor> getById(@PathVariable Long id) {
        return visitorService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Visitor> create(@RequestBody Visitor visitor) {
        return ResponseEntity.ok(visitorService.save(visitor));
    }
}
