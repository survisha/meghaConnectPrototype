package gov.meghalaya.meghaconnect.repository;

import gov.meghalaya.meghaconnect.entity.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, Long>, JpaSpecificationExecutor<Direction> {
}
