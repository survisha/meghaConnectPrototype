package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyPersonIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface LegacyPersonIndexRepository extends JpaRepository<LegacyPersonIndex,Long>, JpaSpecificationExecutor<LegacyPersonIndex> {}
