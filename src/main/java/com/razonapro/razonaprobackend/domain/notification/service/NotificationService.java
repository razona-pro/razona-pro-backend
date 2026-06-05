// domain/notification/service/NotificationService.java
package com.razonapro.razonaprobackend.domain.notification.service;

import com.razonapro.razonaprobackend.domain.notification.dto.NotificationDto;
import com.razonapro.razonaprobackend.domain.notification.model.Notification;
import com.razonapro.razonaprobackend.domain.notification.repository.NotificationRepository;
import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.infrastructure.email.EmailService;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final StudentRepository      studentRepository;
    private final EmailService           emailService;

    // ── Lectura ──
    public PagedResponse<NotificationDto> findMine(UserPrincipal p, Pageable pageable) {
        return PagedResponse.from(repo
                .findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(p.getId(), p.getUserType(), pageable)
                .map(NotificationDto::from));
    }

    public long unreadCount(UserPrincipal p) {
        return repo.countByRecipientIdAndRecipientTypeAndIsReadFalse(p.getId(), p.getUserType());
    }

    @Transactional
    public void markAllRead(UserPrincipal p) {
        repo.markAllRead(p.getId(), p.getUserType());
    }

    @Transactional
    public void markRead(String id, UserPrincipal p) {
        Notification n = repo.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!n.getRecipientId().equals(p.getId()))
            throw new ApiException(ErrorCode.INSUFFICIENT_PERMS);
        n.setIsRead(true);
        n.setReadAt(java.time.LocalDateTime.now());
        repo.save(n);
    }

    // ── Creación individual (in-app) ──
    @Transactional
    public void notify(String recipientId, String recipientType, String type,
                       String title, String body, String link) {
        repo.save(Notification.builder()
                .notificationId(IdGenerator.notificationId(repo.count()))
                .recipientId(recipientId).recipientType(recipientType)
                .type(type).title(title).body(body).link(link)
                .build());
    }

    // ── Nuevo test → broadcast a todos los estudiantes activos ──
    @Async
    @Transactional
    public void broadcastNewTest(String testName, String competenceName) {
        List<Student> students = studentRepository.findByIsActiveTrue();
        String title = "Nuevo test disponible";
        String body  = "Se publicó \"" + testName + "\" en " + competenceName + ". ¡Practica ya!";
        long base = repo.count();
        int i = 0;
        for (Student s : students) {
            repo.save(Notification.builder()
                    .notificationId(IdGenerator.notificationId(base + i++))
                    .recipientId(s.getStudentId()).recipientType("STUDENT")
                    .type("NEW_TEST").title(title).body(body).link("/tests")
                    .build());
            if (Boolean.TRUE.equals(s.getEmailVerified()))
                emailService.sendNewTestEmail(s.getEmail(), s.getFirstName(), testName, competenceName);
        }
        log.info("broadcastNewTest — {} estudiantes notificados sobre '{}'", students.size(), testName);
    }
}