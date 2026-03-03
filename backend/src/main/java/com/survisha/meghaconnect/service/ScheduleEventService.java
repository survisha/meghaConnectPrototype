package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.ScheduleEvent;
import com.survisha.meghaconnect.repository.ScheduleEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleEventService {

    private final ScheduleEventRepository scheduleEventRepository;

    public List<ScheduleEvent> findAll() {
        return scheduleEventRepository.findAll();
    }

    public Optional<ScheduleEvent> findById(Long id) {
        return scheduleEventRepository.findById(id);
    }

    @Transactional
    public ScheduleEvent create(ScheduleEvent event) {
        // Check for conflicts
        boolean conflict = scheduleEventRepository.findAll().stream()
            .anyMatch(e -> !e.getId().equals(event.getId()) &&
                e.getStartTime().isBefore(event.getEndTime()) &&
                event.getStartTime().isBefore(e.getEndTime()));
        event.setConflict(conflict);
        return scheduleEventRepository.save(event);
    }

    @Transactional
    public ScheduleEvent update(ScheduleEvent event) {
        return scheduleEventRepository.save(event);
    }

    @Transactional
    public void delete(Long id) {
        scheduleEventRepository.deleteById(id);
    }
}
