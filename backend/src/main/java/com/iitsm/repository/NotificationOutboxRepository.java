package com.iitsm.repository;

import com.iitsm.domain.model.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findByDeliveredFalseOrderByCreatedAtAsc();

    List<NotificationOutbox> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
}
