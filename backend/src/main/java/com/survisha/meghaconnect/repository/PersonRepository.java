package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {
    Optional<Person> findByPhoneNumber(String phoneNumber);
    Optional<Person> findByEpicNumber(String epicNumber);
    Optional<Person> findByAadhaarNumber(String aadhaarNumber);

    @Query("SELECT p FROM Person p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Person> searchByName(String name);

    @Query("SELECT p FROM Person p WHERE p.constituency = :constituency ORDER BY p.fullName")
    List<Person> findByConstituency(String constituency);
}
