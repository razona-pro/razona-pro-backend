// domain/notification/dto/NotificationDto.java
package com.razonapro.razonaprobackend.domain.notification.dto;

import com.razonapro.razonaprobackend.domain.notification.model.Notification;
import java.time.LocalDateTime;

public record NotificationDto(
        String notificationId, String type, String title, String body,
        String link, boolean isRead, LocalDateTime createdAt) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getNotificationId(), n.getType(), n.getTitle(),
                n.getBody(), n.getLink(), Boolean.TRUE.equals(n.getIsRead()), n.getCreatedAt());
    }
}