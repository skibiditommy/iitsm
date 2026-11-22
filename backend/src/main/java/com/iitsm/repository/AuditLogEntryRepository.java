package com.iitsm.repository;

import com.iitsm.domain.model.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, Long> {

    @Query("SELECT a FROM AuditLogEntry a WHERE a.incident.id = :incidentId ORDER BY a.timestamp ASC")
    List<AuditLogEntry> findByIncidentId(@Param("incidentId") Long incidentId);
}
