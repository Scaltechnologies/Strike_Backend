package com.notification_service.repository;

import com.notification_service.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(Long recipientId, String recipientType);

    List<NotificationLog> findByTypeOrderByCreatedAtDesc(String type);

    List<NotificationLog> findByStatusOrderByCreatedAtDesc(String status);
}