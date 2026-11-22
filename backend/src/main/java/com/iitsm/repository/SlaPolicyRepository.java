package com.iitsm.repository;

import com.iitsm.domain.enums.Priority;
import com.iitsm.domain.model.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    @Query("SELECT p FROM SlaPolicy p WHERE p.priority = :priority AND p.service.id = :serviceId")
    Optional<SlaPolicy> findByPriorityAndServiceId(@Param("priority") Priority priority,
                                                    @Param("serviceId") Long serviceId);

    @Query("SELECT p FROM SlaPolicy p WHERE p.priority = :priority AND p.service IS NULL")
    Optional<SlaPolicy> findDefaultByPriority(@Param("priority") Priority priority);
}
