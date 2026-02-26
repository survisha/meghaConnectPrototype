package gov.meghalaya.meghaconnect.controller;

import gov.meghalaya.meghaconnect.entity.ScheduleEvent;
import gov.meghalaya.meghaconnect.service.ScheduleEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ScheduleEventController {

    private final ScheduleEventService scheduleEventService;

    @GetMapping
    public List<ScheduleEvent> getAll() {
        return scheduleEventService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleEvent> getById(@PathVariable Long id) {
        return scheduleEventService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER_JT_SECY','HCM','SAIDUL_OSD','ADMIN')")
    public ScheduleEvent create(@RequestBody ScheduleEvent event) {
        return scheduleEventService.create(event);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER_JT_SECY','HCM','SAIDUL_OSD','ADMIN')")
    public ScheduleEvent update(@PathVariable Long id, @RequestBody ScheduleEvent event) {
        event.setId(id);
        return scheduleEventService.update(event);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER_JT_SECY','HCM','SAIDUL_OSD','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
