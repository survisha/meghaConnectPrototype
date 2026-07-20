package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDepartmentCodeIgnoreCase(String departmentCode);
    boolean existsByDepartmentCodeIgnoreCase(String departmentCode);
}
