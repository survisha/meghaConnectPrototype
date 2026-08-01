package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.DepartmentAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentAccessRequestRepository extends JpaRepository<DepartmentAccessRequest, Long> {
    List<DepartmentAccessRequest> findByRequestStatusOrderBySubmittedAtDesc(DepartmentAccessRequest.Status status);
    List<DepartmentAccessRequest> findAllByOrderBySubmittedAtDesc();
    boolean existsByDepartmentCodeIgnoreCaseAndRequestStatus(String code, DepartmentAccessRequest.Status status);
    Page<DepartmentAccessRequest> findByRequestStatusOrderBySubmittedAtDesc(DepartmentAccessRequest.Status status, Pageable pageable);
    Page<DepartmentAccessRequest> findAllByOrderBySubmittedAtDesc(Pageable pageable);
}
