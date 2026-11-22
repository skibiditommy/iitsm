package com.iitsm.repository;

import com.iitsm.domain.model.ITService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ITServiceRepository extends JpaRepository<ITService, Long> {
    Optional<ITService> findByName(String name);
    boolean existsByName(String name);
}
