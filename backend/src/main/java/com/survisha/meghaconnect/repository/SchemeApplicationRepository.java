package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.SchemeApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeApplicationRepository extends JpaRepository<SchemeApplication, Long>, JpaSpecificationExecutor<SchemeApplication> {

    List<SchemeApplication> findByApplicant_IdAndSchemeTypeOrderByCreatedAtDesc(
            Long applicantId,
            SchemeApplication.SchemeType schemeType
    );
}
