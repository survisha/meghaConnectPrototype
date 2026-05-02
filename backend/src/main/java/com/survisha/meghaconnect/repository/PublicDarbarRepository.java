package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.PublicDarbar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicDarbarRepository extends JpaRepository<PublicDarbar, Long>, JpaSpecificationExecutor<PublicDarbar> {

    Optional<PublicDarbar> findFirstByStatusOrderByDarbarDateAsc(PublicDarbar.DarbarStatus status);

    boolean existsByDarbarDateAndLocationAndStatus(LocalDate darbarDate, String location, PublicDarbar.DarbarStatus status);

    List<PublicDarbar> findByDarbarDateAndLocationAndStatusIn(
            LocalDate darbarDate,
            String location,
            Collection<PublicDarbar.DarbarStatus> statuses
    );

    List<PublicDarbar> findByStatusOrderByDarbarDateAsc(PublicDarbar.DarbarStatus status);
}
