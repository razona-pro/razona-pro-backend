package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.StartAiTriedRequest;
import com.razonapro.razonaprobackend.dtos.request.SubmitAiAnswerRequest;
import com.razonapro.razonaprobackend.dtos.response.AiTriedDto;
import com.razonapro.razonaprobackend.dtos.response.PagedResponse;
import com.razonapro.razonaprobackend.exception.ApiException;
import com.razonapro.razonaprobackend.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.models.AiTried;
import com.razonapro.razonaprobackend.models.AiTriedResponse;
import com.razonapro.razonaprobackend.repositories.AiTriedRepository;
import com.razonapro.razonaprobackend.repositories.AiTriedResponseRepository;
import com.razonapro.razonaprobackend.repositories.CompetenceRepository;
import com.razonapro.razonaprobackend.security.UserPrincipal;
import com.razonapro.razonaprobackend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTriedService {

    private final AiTriedRepository         aiTriedRepository;
    private final AiTriedResponseRepository aiTriedResponseRepository;
    private final CompetenceRepository      competenceRepository;

    public PagedResponse<AiTriedDto> findMy(UserPrincipal principal, Pageable pageable) {
        Page<AiTriedDto> page = aiTriedRepository
            .findByStudentIdAndProgramId(principal.getId(), principal.getProgramId(), pageable)
            .map(AiTriedDto::from);
        return PagedResponse.from(page);
    }

    public AiTriedDto findById(String aiTriedId, UserPrincipal principal) {
        AiTried aiTried = aiTriedRepository.findByAiTriedId(aiTriedId)
            .orElseThrow(() -> new ResourceNotFoundException("AI Intento", aiTriedId));
        assertOwnership(aiTried, principal);
        return AiTriedDto.from(aiTried);
    }

    @Transactional
    public AiTriedDto start(StartAiTriedRequest req, UserPrincipal principal) {
        competenceRepository.findById(req.getCompetenceId())
            .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        AiTried aiTried = AiTried.builder()
            .programId(principal.getProgramId())
            .studentId(principal.getId())
            .aiTriedId(IdGenerator.aiTriedId())
            .totalQuestions(req.getTotalQuestions())
            .description(req.getDescription())
            .build();
        return AiTriedDto.from(aiTriedRepository.save(aiTried));
    }

    @Transactional
    public AiTriedDto submitAnswer(String aiTriedId, SubmitAiAnswerRequest req, UserPrincipal principal) {
        AiTried aiTried = aiTriedRepository.findByAiTriedId(aiTriedId)
            .orElseThrow(() -> new ResourceNotFoundException("AI Intento", aiTriedId));
        assertOwnership(aiTried, principal);

        if (!"IN_PROGRESS".equals(aiTried.getStatus()))
            throw new ApiException("El intento AI no está en progreso");

        competenceRepository.findById(req.getCompetenceId())
            .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        AiTriedResponse response = AiTriedResponse.builder()
            .programId(aiTried.getProgramId())
            .studentId(aiTried.getStudentId())
            .aiTriedId(aiTriedId)
            .aiTriedResponseId(IdGenerator.aiTriedResponseId())
            .questionText(req.getQuestionText())
            .studentAnswer(req.getStudentAnswer())
            .correctAnswer(req.getCorrectAnswer())
            .isCorrect(req.getIsCorrect())
            .answeredAt(LocalDateTime.now())
            .build();
        // Asociar competencia
        response.setCompetence(competenceRepository.findById(req.getCompetenceId()).orElseThrow());
        aiTriedResponseRepository.save(response);

        // Actualizar contadores
        List<AiTriedResponse> responses = aiTriedResponseRepository.findByAiTriedId(aiTriedId);
        long correct = responses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        aiTried.setCorrectAnswers((int) correct);

        // Auto-finish si ya respondió todas
        if (responses.size() >= aiTried.getTotalQuestions()) {
            finishAiTried(aiTried);
        }
        return AiTriedDto.from(aiTriedRepository.save(aiTried));
    }

    @Transactional
    public AiTriedDto finish(String aiTriedId, Integer timeSpentSeconds, UserPrincipal principal) {
        AiTried aiTried = aiTriedRepository.findByAiTriedId(aiTriedId)
            .orElseThrow(() -> new ResourceNotFoundException("AI Intento", aiTriedId));
        assertOwnership(aiTried, principal);

        if (!"IN_PROGRESS".equals(aiTried.getStatus()))
            throw new ApiException("El intento ya fue finalizado");

        if (timeSpentSeconds != null) aiTried.setTimeSpentSeconds(timeSpentSeconds);
        finishAiTried(aiTried);
        return AiTriedDto.from(aiTriedRepository.save(aiTried));
    }

    private void finishAiTried(AiTried aiTried) {
        aiTried.setStatus("FINISHED");
        aiTried.setFinishedAt(LocalDateTime.now());
        if (aiTried.getTotalQuestions() > 0 && aiTried.getCorrectAnswers() != null) {
            BigDecimal score = BigDecimal.valueOf(aiTried.getCorrectAnswers())
                .divide(BigDecimal.valueOf(aiTried.getTotalQuestions()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
            aiTried.setScore(score);
        }
    }

    private void assertOwnership(AiTried aiTried, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (!aiTried.getStudentId().equals(principal.getId())
                || !aiTried.getProgramId().equals(principal.getProgramId()))) {
            throw new ApiException("No tienes acceso a este intento", HttpStatus.FORBIDDEN);
        }
    }
}
