// domain/aitried/service/AiTriedService.java
package com.razonapro.razonaprobackend.domain.aitried.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.AiHintRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.StartAiTriedRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.SubmitAiAnswerRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.response.*;
import com.razonapro.razonaprobackend.domain.aitried.model.AiQuestion;
import com.razonapro.razonaprobackend.domain.aitried.model.AiTried;
import com.razonapro.razonaprobackend.domain.aitried.model.AiTriedResponse;
import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.AiTutor;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiOption;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiQuestionRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedResponseRepository;
import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.infrastructure.ai.AiUnavailableException;
import com.razonapro.razonaprobackend.infrastructure.config.AiModelProperties;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTriedService {

    private final AiTriedRepository         aiTriedRepository;
    private final AiTriedResponseRepository aiTriedResponseRepository;
    private final AiQuestionRepository      aiQuestionRepository;
    private final CompetenceRepository      competenceRepository;
    private final AiQuestionGenerator       questionGenerator;
    private final AiTutor                   aiTutor;
    private final AiModelProperties         aiProps;
    private final ObjectMapper              mapper;

    // ── Consultas ──
    public PagedResponse<AiTriedDto> findMy(UserPrincipal p, Pageable pageable) {
        return PagedResponse.from(aiTriedRepository
                .findByStudentIdAndProgramId(p.getId(), p.getProgramId(), pageable)
                .map(AiTriedDto::from));
    }

    public AiTriedDto findById(String aiTriedId, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        return AiTriedDto.from(at);
    }

    /** Historial detallado: cada pregunta con su resultado. */
    public List<AiQuestionDto> getReview(String aiTriedId, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        List<AiQuestion> qs = aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId);
        List<AiQuestionDto> out = new ArrayList<>();
        for (AiQuestion q : qs) out.add(toDto(q, at.getTotalQuestions(), true));
        return out;
    }

    // ── Estado ──
    public AiStatusDto getStatus() {
        boolean enabled = aiProps.isEnabled();
        String provider = aiProps.getProvider().name();
        String model    = aiProps.getCloudModel();
        boolean reachable = false;
        String message;
        if (!enabled || aiProps.getProvider() == AiModelProperties.Provider.NONE) {
            message = "Módulo IA deshabilitado.";
        } else {
            try {
                reachable = questionGenerator.isAvailable();
                message = reachable ? "Proveedor IA disponible: " + model
                        : "Proveedor IA configurado pero no alcanzable.";
            } catch (Exception e) { message = "Error verificando IA: " + e.getMessage(); }
        }
        return new AiStatusDto(enabled, provider, model, reachable, message);
    }

    // ── Iniciar: genera TODO el batch ──
    @Transactional
    public AiStartResponseDto start(StartAiTriedRequest req, UserPrincipal p) {
        if (!questionGenerator.isAvailable())
            throw new ApiException(ErrorCode.AI_MODULE_DISABLED);

        Competence comp = competenceRepository.findById(req.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        // 1. Generar batch (fuera de cualquier lock largo: una sola llamada IA)
        List<AiGeneratedQuestion> batch;
        try {
            batch = questionGenerator.generateBatch(
                    comp.getCompetenceName(),
                    comp.getDescription() != null ? comp.getDescription() : "",
                    req.getTotalQuestions(), 5);
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED, e.getMessage());
        }
        if (batch.isEmpty()) throw new ApiException(ErrorCode.AI_BATCH_EMPTY);

        int total = Math.min(batch.size(), req.getTotalQuestions());

        // 2. Crear AiTried
        AiTried at = AiTried.builder()
                .programId(p.getProgramId())
                .studentId(p.getId())
                .aiTriedId(IdGenerator.aiTriedId())
                .competenceId(comp.getCompetenceId())
                .totalQuestions(total)
                .description(req.getDescription())
                .build();
        aiTriedRepository.save(at);

        // 3. Persistir todas las preguntas
        for (int i = 0; i < total; i++) {
            AiGeneratedQuestion g = batch.get(i);
            aiQuestionRepository.save(AiQuestion.builder()
                    .aiQuestionId(IdGenerator.aiQuestionId())
                    .programId(at.getProgramId())
                    .studentId(at.getStudentId())
                    .aiTriedId(at.getAiTriedId())
                    .competenceId(comp.getCompetenceId())
                    .questionOrder(i + 1)
                    .statement(g.statement())
                    .optionsJson(writeOptions(g.options()))
                    .correctIndex(g.correctIndex())
                    .explanation(g.explanation())
                    .difficultyLevel(g.difficultyLevel())
                    .build());
        }

        AiQuestion first = aiQuestionRepository
                .findByAiTriedIdOrderByQuestionOrderAsc(at.getAiTriedId()).get(0);
        return new AiStartResponseDto(AiTriedDto.from(at), toDto(first, total, false), total);
    }

    // ── Obtener pregunta por número (navegación) ──
    public List<AiQuestionDto> listQuestions(String aiTriedId, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        List<AiQuestion> qs = aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId);
        List<AiQuestionDto> out = new ArrayList<>();
        for (AiQuestion q : qs) out.add(toDto(q, at.getTotalQuestions(), false));
        return out;
    }

    // ── Responder ──
    @Transactional
    public AiAnswerResultDto submitAnswer(String aiTriedId, SubmitAiAnswerRequest req, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        assertInProgress(at);

        AiQuestion q = aiQuestionRepository
                .findByAiQuestionIdAndAiTriedId(req.getAiQuestionId(), aiTriedId)
                .orElseThrow(() -> new ApiException(ErrorCode.AI_QUESTION_NOT_FOUND));

        if (q.getSelectedIndex() != null)
            throw new ApiException(ErrorCode.AI_ALREADY_ANSWERED);

        List<AiOption> options = readOptions(q.getOptionsJson());
        int sel = req.getSelectedIndex();
        if (sel < 0 || sel >= options.size())
            throw new ApiException(ErrorCode.AI_INVALID_OPTION);

        boolean isCorrect = (sel == q.getCorrectIndex());

        // Persistir en ai_questions
        q.setSelectedIndex(sel);
        q.setIsCorrect(isCorrect);
        q.setAnsweredAt(LocalDateTime.now());
        aiQuestionRepository.save(q);

        // Persistir en ai_tried_responses (dispara triggers de correct_answers)
        Competence comp = competenceRepository.findById(q.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", q.getCompetenceId()));
        aiTriedResponseRepository.save(AiTriedResponse.builder()
                .programId(at.getProgramId())
                .studentId(at.getStudentId())
                .aiTriedId(aiTriedId)
                .aiTriedResponseId(IdGenerator.aiTriedResponseId())
                .questionText(truncate(q.getStatement(), 1990))
                .studentAnswer(truncate(options.get(sel).text(), 290))
                .correctAnswer(truncate(options.get(q.getCorrectIndex()).text(), 290))
                .isCorrect(isCorrect)
                .answeredAt(LocalDateTime.now())
                .competence(comp)
                .build());

        // Recontar respondidas y correctas
        long answered = aiQuestionRepository.countByAiTriedIdAndSelectedIndexIsNotNull(aiTriedId);
        long correct  = aiTriedResponseRepository.findByAiTriedId(aiTriedId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        at.setCorrectAnswers((int) correct);

        boolean finished = answered >= at.getTotalQuestions();
        Double finalScore = null;
        if (finished) {
            finishAiTried(at);
            finalScore = at.getScore() != null ? at.getScore().doubleValue() : 0.0;
        }
        aiTriedRepository.save(at);

        return new AiAnswerResultDto(isCorrect, sel, q.getCorrectIndex(), q.getExplanation(),
                (int) correct, (int) answered, at.getTotalQuestions(), finished, finalScore);
    }

    // ── Pista ──
    @Transactional
    public AiHintDto getHint(String aiTriedId, AiHintRequest req, UserPrincipal p) {
        if (!aiTutor.isAvailable())
            throw new ApiException(ErrorCode.AI_TUTOR_DISABLED);

        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        assertInProgress(at);

        AiQuestion q = aiQuestionRepository
                .findByAiQuestionIdAndAiTriedId(req.getAiQuestionId(), aiTriedId)
                .orElseThrow(() -> new ApiException(ErrorCode.AI_QUESTION_NOT_FOUND));

        // El front bloquea botones inferiores; backend valida progresión y tope
        if (req.getHintLevel() > q.getHintsUsed() + 1)
            throw new ApiException(ErrorCode.AI_INVALID_OPTION, "Debes pedir las pistas en orden.");
        if (q.getHintsUsed() >= 3)
            throw new ApiException(ErrorCode.HINT_LIMIT_REACHED);

        List<AiOption> options = readOptions(q.getOptionsJson());
        List<String> texts = new ArrayList<>();
        for (AiOption o : options) texts.add(o.text());

        AiHintContext ctx = new AiHintContext(
                q.getCompetenceId(), q.getStatement(), texts,
                options.get(q.getCorrectIndex()).text(), req.getHintLevel());

        String hint;
        try {
            hint = aiTutor.generateHint(ctx);
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED, e.getMessage());
        }

        q.setHintsUsed(Math.max(q.getHintsUsed(), req.getHintLevel()));
        aiQuestionRepository.save(q);
        return new AiHintDto(hint, req.getHintLevel());
    }

    // ── Finalizar manual ──
    @Transactional
    public AiTriedDto finish(String aiTriedId, Integer timeSpentSeconds, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        if (!"IN_PROGRESS".equals(at.getStatus()))
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED);
        if (timeSpentSeconds != null) at.setTimeSpentSeconds(timeSpentSeconds);
        finishAiTried(at);
        return AiTriedDto.from(aiTriedRepository.save(at));
    }

    // ── Helpers ──
    private void finishAiTried(AiTried at) {
        at.setStatus("FINISHED");
        at.setFinishedAt(LocalDateTime.now());
        if (at.getTotalQuestions() > 0 && at.getCorrectAnswers() != null) {
            at.setScore(BigDecimal.valueOf(at.getCorrectAnswers())
                    .divide(BigDecimal.valueOf(at.getTotalQuestions()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private AiQuestionDto toDto(AiQuestion q, int total, boolean reveal) {
        List<AiOption> options = readOptions(q.getOptionsJson());
        List<AiQuestionDto.OptionDto> opts = new ArrayList<>();
        for (int i = 0; i < options.size(); i++)
            opts.add(new AiQuestionDto.OptionDto("OPT" + i, options.get(i).text()));
        return new AiQuestionDto(
                q.getAiQuestionId(), q.getStatement(), opts, q.getDifficultyLevel(),
                q.getQuestionOrder(), total, q.getHintsUsed(),
                q.getSelectedIndex(),
                reveal ? q.getIsCorrect() : (q.getSelectedIndex() != null ? q.getIsCorrect() : null));
    }

    private String writeOptions(List<AiOption> opts) {
        try { return mapper.writeValueAsString(opts); }
        catch (Exception e) { throw new ApiException(ErrorCode.INTERNAL_ERROR, "Error serializando opciones"); }
    }

    private List<AiOption> readOptions(String json) {
        try { return mapper.readValue(json, new TypeReference<List<AiOption>>() {}); }
        catch (Exception e) { throw new ApiException(ErrorCode.INTERNAL_ERROR, "Error leyendo opciones"); }
    }

    private AiTried getOrThrow(String id) {
        return aiTriedRepository.findByAiTriedId(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI Intento", id));
    }

    private void assertOwnership(AiTried at, UserPrincipal p) {
        boolean admin = p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && (!at.getStudentId().equals(p.getId()) || !at.getProgramId().equals(p.getProgramId())))
            throw new ApiException(ErrorCode.INSUFFICIENT_PERMS);
    }

    private void assertInProgress(AiTried at) {
        if (!"IN_PROGRESS".equals(at.getStatus()))
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED,
                    "Este intento ya está " + at.getStatus().toLowerCase() + ".");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}