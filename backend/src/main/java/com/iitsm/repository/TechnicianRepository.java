package com.iitsm.repository;

import com.iitsm.domain.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    Optional<Technician> findByEmail(String email);

    @Query("SELECT t FROM Technician t WHERE t.available = true AND t.currentWorkload < t.maxWorkload")
    List<Technician> findAvailableTechnicians();

    List<Technician> findByAvailableTrue();
}
