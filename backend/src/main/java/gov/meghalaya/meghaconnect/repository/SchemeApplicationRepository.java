package gov.meghalaya.meghaconnect.repository;

import gov.meghalaya.meghaconnect.entity.SchemeApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeApplicationRepository extends JpaRepository<SchemeApplication, Long>, JpaSpecificationExecutor<SchemeApplication> {
}
