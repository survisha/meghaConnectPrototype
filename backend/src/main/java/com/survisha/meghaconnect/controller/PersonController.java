package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Person;
import com.survisha.meghaconnect.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PersonController {

    private final PersonService personService;

    @GetMapping("/search/phone/{phone}")
    public ResponseEntity<Person> findByPhone(@PathVariable String phone) {
        return personService.findByPhone(phone)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/epic/{epic}")
    public ResponseEntity<Person> findByEpic(@PathVariable String epic) {
        return personService.findByEpic(epic)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Person>> searchByName(@RequestParam String q) {
        return ResponseEntity.ok(personService.searchByName(q));
    }

    @GetMapping("/search/district/{district}")
    public ResponseEntity<List<Person>> findByDistrict(@PathVariable String district) {
        return ResponseEntity.ok(personService.findByDistrict(district));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Person> getById(@PathVariable Long id) {
        return personService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Person> create(@RequestBody Person person) {
        return ResponseEntity.ok(personService.save(person));
    }
}
