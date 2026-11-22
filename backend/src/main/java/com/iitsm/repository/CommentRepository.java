package com.iitsm.repository;

import com.iitsm.domain.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.incident.id = :incidentId ORDER BY c.createdAt ASC")
    List<Comment> findByIncidentId(@Param("incidentId") Long incidentId);
}
