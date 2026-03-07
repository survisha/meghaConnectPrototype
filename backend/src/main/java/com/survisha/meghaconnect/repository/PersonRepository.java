package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {
    Optional<Person> findByPhoneNumber(String phoneNumber);
    Optional<Person> findByEpicNumber(String epicNumber);
    Optional<Person> findByAadhaarNumber(String aadhaarNumber);

    @Query("SELECT p FROM Person p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Person> searchByName(@Param("name") String name);

    @Query("SELECT p FROM Person p WHERE p.constituency = :constituency ORDER BY p.fullName")
    List<Person> findByConstituency(@Param("constituency") String constituency);

    @Query("SELECT p FROM Person p WHERE p.district = :district ORDER BY p.fullName")
    List<Person> findByDistrict(@Param("district") String district);
}
