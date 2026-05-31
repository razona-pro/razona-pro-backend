// src/main/java/com/razonapro/razonaprobackend/domain/notification/model/Notification.java
package com.razonapro.razonaprobackend.domain.notification.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "notification_id", length = 12)
    private String notificationId;

    @Column(name = "recipient_id",   length = 7,   nullable = false) private String recipientId;
    @Column(name = "recipient_type", length = 10,  nullable = false) private String recipientType; // STUDENT|ADMIN
    @Column(name = "type",           length = 20,  nullable = false) private String type;          // NEW_TEST|DOUBT_REPORT|SYSTEM
    @Column(name = "title",          length = 120, nullable = false) private String title;
    @Column(name = "body",           length = 500) private String body;
    @Column(name = "link",           length = 200) private String link;

    @Column(name = "is_read", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at") private LocalDateTime readAt;
}