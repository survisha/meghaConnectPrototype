package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferenceTypeRepository extends JpaRepository<ReferenceType, Long> {

    Optional<ReferenceType> findByCode(String code);

    boolean existsByCode(String code);
}