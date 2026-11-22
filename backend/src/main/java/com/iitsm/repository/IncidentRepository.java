package com.iitsm.repository;

import com.iitsm.domain.enums.SlaStatus;
import com.iitsm.domain.enums.Status;
import com.iitsm.domain.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query(value = "SELECT nextval('incident_display_number')", nativeQuery = true)
    long nextDisplayNumber();

    boolean existsByIncidentId(String incidentId);

    Optional<Incident> findByIncidentId(String incidentId);

    List<Incident> findByStatus(Status status);

    @Query("SELECT i FROM Incident i WHERE i.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED')")
    List<Incident> findActiveIncidents();

    @Query("SELECT i FROM Incident i WHERE i.responseSlaStatus = :slaStatus OR i.resolutionSlaStatus = :slaStatus")
    List<Incident> findBySlaStatus(@Param("slaStatus") SlaStatus slaStatus);

    @Query("SELECT i FROM Incident i WHERE i.reporter.id = :reporterId ORDER BY i.createdAt DESC")
    List<Incident> findByReporterId(@Param("reporterId") Long reporterId);

    @Query("SELECT i FROM Incident i WHERE i.assignedTechnician.id = :technicianId ORDER BY i.createdAt DESC")
    List<Incident> findByAssignedTechnicianId(@Param("technicianId") Long technicianId);

    @Query("SELECT i FROM Incident i WHERE i.category.id = :categoryId AND i.status = 'CLOSED'")
    List<Incident> findResolvedByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT i FROM Incident i WHERE i.affectedService.id = :serviceId")
    List<Incident> findByAffectedServiceId(@Param("serviceId") Long serviceId);

    @Query("SELECT i FROM Incident i WHERE i.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED') " +
            "AND (i.responseSlaStatus = 'BREACHED' OR i.resolutionSlaStatus = 'BREACHED')")
    List<Incident> findActiveSlaBreached();

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status = :status")
    long countByStatus(@Param("status") Status status);

    @Query("SELECT i FROM Incident i WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Incident> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT i FROM Incident i WHERE i.category.id = :categoryId AND i.status = 'CLOSED' " +
            "AND i.resolution IS NOT NULL ORDER BY i.resolvedAt DESC")
    List<Incident> findResolvedWithResolutionByCategory(@Param("categoryId") Long categoryId);
}
