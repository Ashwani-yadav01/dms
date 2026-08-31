package com.dms.notificationService.repository;

import com.dms.notificationService.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByRecipientEmail(String recipientEmail);
    List<NotificationLog> findByAlertType(String alertType);
}