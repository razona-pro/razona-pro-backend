package com.razonapro.razonaprobackend.domain.aitried.service;

import com.razonapro.razonaprobackend.domain.aitried.dto.request.StartAiTriedRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.SubmitAiAnswerRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.response.AiTriedDto;
import com.razonapro.razonaprobackend.domain.aitried.model.AiTried;
import com.razonapro.razonaprobackend.domain.aitried.model.AiTriedResponse;
import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedResponseRepository;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTriedService {

    private final AiTriedRepository         aiTriedRepository;
    private final AiTriedResponseRepository aiTriedResponseRepository;
    private final CompetenceRepository      competenceRepository;
    private final AiQuestionGenerator       aiQuestionGenerator;

    public PagedResponse<AiTriedDto> findMy(UserPrincipal principal, Pageable pageable) {
        return PagedResponse.from(aiTriedRepository
                .findByStudentIdAndProgramId(principal.getId(), principal.getProgramId(), pageable)
                .map(AiTriedDto::from));
    }

    public AiTriedDto findById(String aiTriedId, UserPrincipal principal) {
        AiTried at = aiTriedRepository.findByAiTriedId(aiTriedId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Intento", aiTriedId));
        assertOwnership(at, principal);
        return AiTriedDto.from(at);
    }

    @Transactional
    public AiTriedDto start(StartAiTriedRequest req, UserPrincipal principal) {
        competenceRepository.findById(req.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        log.debug("Iniciando AI Tried — generator available: {}", aiQuestionGenerator.isAvailable());

        AiTried at = AiTried.builder()
                .programId(principal.getProgramId())
                .studentId(principal.getId())
                .aiTriedId(IdGenerator.aiTriedId())
                .totalQuestions(req.getTotalQuestions())
                .description(req.getDescription())
                .build();
        return AiTriedDto.from(aiTriedRepository.save(at));
    }

    @Transactional
    public AiTriedDto submitAnswer(String aiTriedId, SubmitAiAnswerRequest req, UserPrincipal principal) {
        AiTried at = aiTriedRepository.findByAiTriedId(aiTriedId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Intento", aiTriedId));
        assertOwnership(at, principal);

        if (!"IN_PROGRESS".equals(at.getStatus()))
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED);

        var competence = competenceRepository.findById(req.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        AiTriedResponse response = AiTriedResponse.builder()
                .programId(at.getProgramId())
                .studentId(at.getStudentId())
                .aiTriedId(aiTriedId)
                .aiTriedResponseId(IdGenerator.aiTriedResponseId())
                .questionText(req.getQuestionText())
                .studentAnswer(req.getStudentAnswer())
                .correctAnswer(req.getCorrectAnswer())
                .isCorrect(req.getIsCorrect())
                .answeredAt(LocalDateTime.now())
                .build();
        response.setCompetence(competence);
        aiTriedResponseRepository.save(response);

        List<AiTriedResponse> all = aiTriedResponseRepository.findByAiTriedId(aiTriedId);
        long correct = all.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        at.setCorrectAnswers((int) correct);

        if (all.size() >= at.getTotalQuestions()) {
            finishAiTried(at);
        }
        return AiTriedDto.from(aiTriedRepository.save(at));
    }

    @Transactional
    public AiTriedDto finish(String aiTriedId, Integer timeSpentSeconds, UserPrincipal principal) {
        AiTried at = aiTriedRepository.findByAiTriedId(aiTriedId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Intento", aiTriedId));
        assertOwnership(at, principal);

        if (!"IN_PROGRESS".equals(at.getStatus()))
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED);

        if (timeSpentSeconds != null) at.setTimeSpentSeconds(timeSpentSeconds);
        finishAiTried(at);
        return AiTriedDto.from(aiTriedRepository.save(at));
    }

    private void finishAiTried(AiTried at) {
        at.setStatus("FINISHED");
        at.setFinishedAt(LocalDateTime.now());
        if (at.getTotalQuestions() > 0 && at.getCorrectAnswers() != null) {
            BigDecimal score = BigDecimal.valueOf(at.getCorrectAnswers())
                    .divide(BigDecimal.valueOf(at.getTotalQuestions()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            at.setScore(score);
        }
    }

    private void assertOwnership(AiTried at, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (!at.getStudentId().equals(principal.getId())
                || !at.getProgramId().equals(principal.getProgramId()))) {
            throw new ApiException(ErrorCode.INSUFFICIENT_PERMS);
        }
    }
}