// src/main/java/com/razonapro/razonaprobackend/domain/notification/repository/NotificationRepository.java
package com.razonapro.razonaprobackend.domain.notification.repository;

import com.razonapro.razonaprobackend.domain.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(
            String recipientId, String recipientType, Pageable pageable);

    long countByRecipientIdAndRecipientTypeAndIsReadFalse(String recipientId, String recipientType);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.recipientId = :rid AND n.recipientType = :rtype AND n.isRead = false")
    int markAllRead(String rid, String rtype);
}