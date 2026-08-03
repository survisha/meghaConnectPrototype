package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long>, JpaSpecificationExecutor<Visitor> {
    List<Visitor> findByPhoneNumber(String phoneNumber);
    List<Visitor> findByPhoneNumberAndEpicNumber(String phoneNumber, String epicNumber);
    Optional<Visitor> findByEpicNumber(String epicNumber);
    Optional<Visitor> findByEpicNumberIgnoreCase(String epicNumber);
    Optional<Visitor> findByAadhaarNumber(String aadhaarNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEpicNumber(String epicNumber);
    boolean existsByEpicNumberAndPhoneNumber(String epicNumber, String phoneNumber);

    @Query("SELECT v FROM Visitor v WHERE LOWER(v.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Visitor> searchByName(@Param("name") String name);

    @Query("SELECT v FROM Visitor v WHERE " +
            "LOWER(v.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR v.phoneNumber LIKE CONCAT('%', :query, '%') " +
            "OR UPPER(v.epicNumber) = UPPER(:query) " +
            "OR v.maskedIdentityNumber LIKE CONCAT('%', :query, '%') " +
            "ORDER BY v.fullName")
    List<Visitor> searchRegisteredCitizens(@Param("query") String query);

    @Query("SELECT v FROM Visitor v WHERE v.constituency = :constituency ORDER BY v.fullName")
    List<Visitor> findByConstituency(@Param("constituency") String constituency);

    @Query("SELECT v FROM Visitor v WHERE v.district = :district ORDER BY v.fullName")
    List<Visitor> findByDistrict(@Param("district") String district);
}
