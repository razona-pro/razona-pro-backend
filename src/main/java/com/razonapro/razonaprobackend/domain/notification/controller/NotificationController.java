// domain/notification/controller/NotificationController.java
package com.razonapro.razonaprobackend.domain.notification.controller;

import com.razonapro.razonaprobackend.domain.notification.dto.NotificationDto;
import com.razonapro.razonaprobackend.domain.notification.service.NotificationService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
@Tag(name = "Notifications", description = "Notificaciones in-app")
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> mine(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.findMine(p, PageRequest.of(page, size))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unread(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", service.unreadCount(p))));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> readAll(@AuthenticationPrincipal UserPrincipal p) {
        service.markAllRead(p);
        return ResponseEntity.ok(ApiResponse.ok("Todas marcadas como leídas"));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> read(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal p) {
        service.markRead(id, p);
        return ResponseEntity.ok(ApiResponse.ok("Marcada como leída"));
    }
}