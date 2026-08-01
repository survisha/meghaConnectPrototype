package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import javax.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @EntityGraph(attributePaths = "department")
    @Query("select u from User u where lower(trim(u.username)) = lower(trim(:username))")
    Optional<User> findByNormalizedUsername(@Param("username") String username);
    @EntityGraph(attributePaths = "department")
    @Query("select u from User u where lower(trim(u.username)) = lower(trim(:username)) order by u.id asc")
    List<User> findAllByNormalizedUsername(@Param("username") String username);
    @Query("select count(u) > 0 from User u where lower(trim(u.username)) = lower(trim(:username))")
    boolean existsByNormalizedUsername(@Param("username") String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    List<User> findByDepartment_Id(Long departmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u left join fetch u.department where lower(trim(u.username)) = lower(trim(:username))")
    Optional<User> findForLoginUpdate(@Param("username") String username);
}
