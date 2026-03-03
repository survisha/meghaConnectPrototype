package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.ScheduleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, Long>, JpaSpecificationExecutor<ScheduleEvent> {
}
