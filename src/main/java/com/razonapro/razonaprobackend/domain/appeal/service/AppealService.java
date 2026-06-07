package com.razonapro.razonaprobackend.domain.appeal.service;

import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.appeal.dto.*;
import com.razonapro.razonaprobackend.domain.appeal.model.Appeal;
import com.razonapro.razonaprobackend.domain.appeal.repository.AppealRepository;
import com.razonapro.razonaprobackend.domain.notification.service.NotificationService;
import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.infrastructure.email.EmailService;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppealService {

    private static final String STUDENT_CODE_PATTERN = "^[0-9]{7}$";
    private static final String STATUS_PENDING  = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final AppealRepository    appealRepository;
    private final StudentRepository   studentRepository;
    private final AdminRepository     adminRepository;
    private final NotificationService notificationService;
    private final EmailService        emailService;
    private final PasswordEncoder     passwordEncoder;

    // ── Estado de cuenta (público, re-valida credenciales) ───────────────

    @Transactional(readOnly = true)
    public AccountStatusDto accountStatus(AccountStatusRequest req) {
        Student s = verifyCredentials(req.getCode(), req.getEmail(), req.getPassword());

        if (Boolean.TRUE.equals(s.getIsActive())) {
            return AccountStatusDto.builder().active(true).build();
        }

        boolean pending = appealRepository.existsByStudentIdAndProgramIdAndStatus(
                s.getStudentId(), s.getProgramId(), STATUS_PENDING);
        var last = appealRepository.findTopByStudentIdAndProgramIdOrderByCreatedAtDesc(
                s.getStudentId(), s.getProgramId());

        return AccountStatusDto.builder()
                .active(false)
                .deactivationReason(s.getDeactivationReason() == null ? "MANUAL" : s.getDeactivationReason())
                .hasPendingAppeal(pending)
                .lastAppealStatus(last.map(Appeal::getStatus).orElse(null))
                .lastAdminResponse(last.map(Appeal::getAdminResponse).orElse(null))
                .build();
    }

    // ── Enviar apelación (público) ───────────────────────────────────────

    @Transactional
    public AppealDto submit(AppealSubmitRequest req) {
        Student s = verifyCredentials(req.getCode(), req.getEmail(), req.getPassword());

        if (Boolean.TRUE.equals(s.getIsActive()))
            throw new ApiException(ErrorCode.ACCOUNT_ALREADY_ACTIVE);

        if (appealRepository.existsByStudentIdAndProgramIdAndStatus(
                s.getStudentId(), s.getProgramId(), STATUS_PENDING))
            throw new ApiException(ErrorCode.APPEAL_ALREADY_PENDING);

        Appeal appeal = appealRepository.save(Appeal.builder()
                .appealId(IdGenerator.appealId(appealRepository.count()))
                .studentId(s.getStudentId())
                .programId(s.getProgramId())
                .deactivationReason(s.getDeactivationReason() == null ? "MANUAL" : s.getDeactivationReason())
                .message(req.getMessage().trim())
                .status(STATUS_PENDING)
                .build());

        notifyAdminsNewAppeal(s, appeal.getMessage());
        log.info("Appeal submitted by {}/{}", s.getProgramId(), s.getStudentId());
        return AppealDto.from(appeal, fullName(s));
    }

    // ── Admin: listar ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<AppealDto> findAll(String status, Pageable pageable) {
        var page = (status == null || status.isBlank())
                ? appealRepository.findAllByOrderByCreatedAtDesc(pageable)
                : appealRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable);
        return PagedResponse.from(page.map(a -> AppealDto.from(a,
                studentRepository.findByStudentId(a.getStudentId()).map(this::fullName).orElse(null))));
    }

    // ── Admin: resolver (aprobar reactiva la cuenta) ─────────────────────

    @Transactional
    public AppealDto resolve(String appealId, AppealResolveRequest req, String adminId) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ApiException(ErrorCode.APPEAL_NOT_FOUND));

        if (!STATUS_PENDING.equals(appeal.getStatus()))
            throw new ApiException(ErrorCode.APPEAL_ALREADY_RESOLVED);

        boolean approve = Boolean.TRUE.equals(req.getApprove());
        appeal.setStatus(approve ? STATUS_APPROVED : STATUS_REJECTED);
        appeal.setAdminResponse(req.getResponse() == null || req.getResponse().isBlank()
                ? null : req.getResponse().trim());
        appeal.setResolvedBy(adminId);
        appeal.setResolvedAt(LocalDateTime.now());
        appealRepository.save(appeal);

        Student s = studentRepository.findByStudentId(appeal.getStudentId()).orElse(null);
        if (approve && s != null) {
            s.setIsActive(true);
            s.setDeactivationReason(null);
            studentRepository.save(s);
        }

        // Correo al estudiante con el resultado (best-effort, async).
        if (s != null) {
            try { emailService.sendAppealResolvedEmail(s.getEmail(), s.getFirstName(), approve, appeal.getAdminResponse()); }
            catch (Exception ignored) { /* correo no crítico */ }
        }

        // Notificación best-effort (corre en tx propia; no rompe la resolución).
        if (s != null) {
            try {
                if (approve) {
                    notificationService.notify(s.getStudentId(), "STUDENT", "APPEAL_APPROVED",
                            "Apelación aprobada",
                            "Tu apelación fue aprobada y tu cuenta fue reactivada. Ya puedes iniciar sesión."
                                    + (appeal.getAdminResponse() != null ? " Nota: " + appeal.getAdminResponse() : ""),
                            "/auth");
                } else {
                    notificationService.notify(s.getStudentId(), "STUDENT", "APPEAL_REJECTED",
                            "Apelación rechazada",
                            "Tu apelación fue rechazada y tu cuenta sigue desactivada."
                                    + (appeal.getAdminResponse() != null ? " Motivo: " + appeal.getAdminResponse() : ""),
                            "/auth");
                }
            } catch (Exception ignored) { /* notificación no crítica */ }
        }

        log.info("Appeal {} resolved as {} by {}", appealId, appeal.getStatus(), adminId);
        return AppealDto.from(appeal, s != null ? fullName(s) : null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Valida código(7 díg.)+correo+contraseña de un estudiante. Lanza INVALID_CREDENTIALS si algo no cuadra. */
    private Student verifyCredentials(String rawCode, String rawEmail, String password) {
        String code  = rawCode  == null ? "" : rawCode.trim().toUpperCase();
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();

        if (!code.matches(STUDENT_CODE_PATTERN))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);

        Student s = studentRepository.findByStudentId(code)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (!s.getEmail().equalsIgnoreCase(email))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        if (!passwordEncoder.matches(password, s.getPasswordHash()))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        return s;
    }

    private void notifyAdminsNewAppeal(Student s, String message) {
        String name = fullName(s);
        String reason = s.getDeactivationReason() == null ? "MANUAL" : s.getDeactivationReason();
        adminRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .forEach(a -> {
                    // Notificación in-app (best-effort).
                    try {
                        notificationService.notify(a.getAdminId(), "ADMIN", "APPEAL_NEW",
                                "Nueva apelación de estudiante",
                                "El estudiante " + name + " (" + s.getStudentId() + ") envió una apelación de reactivación.",
                                "/admin/appeals");
                    } catch (Exception ignored) { /* notificación no crítica */ }
                    // Correo (best-effort, async).
                    try {
                        emailService.sendAppealReceivedAdminEmail(
                                a.getEmail(), a.getFirstName(), name, s.getStudentId(), reason, message);
                    } catch (Exception ignored) { /* correo no crítico */ }
                });
    }

    private String fullName(Student s) {
        return (s.getFirstName() + " " + s.getFirstSurname()).trim();
    }
}
