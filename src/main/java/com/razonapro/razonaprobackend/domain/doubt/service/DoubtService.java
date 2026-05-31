// domain/doubt/service/DoubtService.java
package com.razonapro.razonaprobackend.domain.doubt.service;

import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiQuestionRepository;
import com.razonapro.razonaprobackend.domain.doubt.dto.DoubtDto;
import com.razonapro.razonaprobackend.domain.doubt.dto.DoubtRequest;
import com.razonapro.razonaprobackend.domain.doubt.model.QuestionDoubt;
import com.razonapro.razonaprobackend.domain.doubt.repository.QuestionDoubtRepository;
import com.razonapro.razonaprobackend.domain.notification.service.NotificationService;
import com.razonapro.razonaprobackend.domain.question.repository.QuestionRepository;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DoubtService {

    private final QuestionDoubtRepository repo;
    private final QuestionRepository      questionRepository;
    private final AiQuestionRepository    aiQuestionRepository;
    private final AdminRepository         adminRepository;
    private final NotificationService     notificationService;

    @Transactional
    public DoubtDto report(DoubtRequest req, UserPrincipal p) {
        String statement = resolveStatement(req);

        QuestionDoubt d = repo.save(QuestionDoubt.builder()
                .doubtId(IdGenerator.doubtId())
                .studentId(p.getId()).programId(p.getProgramId())
                .source(req.getSource()).competenceId(req.getCompetenceId())
                .questionId(req.getQuestionId()).aiQuestionId(req.getAiQuestionId())
                .statement(statement).message(req.getMessage())
                .build());

        // Notificar a todos los admins activos
        adminRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .forEach(a -> notificationService.notify(
                        a.getAdminId(), "ADMIN", "DOUBT_REPORT",
                        "Nueva duda reportada",
                        "El estudiante " + p.getId() + " reportó una duda (" + req.getSource() + ").",
                        "/admin/doubts"));
        return DoubtDto.from(d);
    }

    public PagedResponse<DoubtDto> findAll(String status, Pageable pageable) {
        var page = (status == null || status.isBlank())
                ? repo.findAllByOrderByCreatedAtDesc(pageable)
                : repo.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        return PagedResponse.from(page.map(DoubtDto::from));
    }

    @Transactional
    public DoubtDto updateStatus(String doubtId, String status) {
        QuestionDoubt d = repo.findById(doubtId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOUBT_NOT_FOUND));
        d.setStatus(status.toUpperCase());
        if (!"OPEN".equalsIgnoreCase(status)) d.setReviewedAt(LocalDateTime.now());
        return DoubtDto.from(repo.save(d));
    }

    private String resolveStatement(DoubtRequest req) {
        if ("STATIC".equals(req.getSource()) && req.getCompetenceId() != null && req.getQuestionId() != null) {
            return questionRepository.findById(new QuestionId(req.getCompetenceId(), req.getQuestionId()))
                    .map(q -> q.getStatement()).orElse(null);
        }
        if ("AI".equals(req.getSource()) && req.getAiQuestionId() != null) {
            return aiQuestionRepository.findById(req.getAiQuestionId())
                    .map(q -> q.getStatement()).orElse(null);
        }
        return null;
    }
}