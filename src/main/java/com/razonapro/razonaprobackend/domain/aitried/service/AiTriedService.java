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
import com.razonapro.razonaprobackend.domain.aitried.model.AiUserCompetence;
import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.AiTutor;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiGeneratedQuestion;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiOption;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiQuestionRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedResponseRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiUserCompetenceRepository;
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
import com.razonapro.razonaprobackend.shared.ids.AiUserCompetenceId;
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
    private final AiUserCompetenceRepository aiUserCompetenceRepository;
    private final CompetenceRepository      competenceRepository;
    private final AiQuestionGenerator       questionGenerator;
    private final AiTutor                   aiTutor;
    private final AiModelProperties         aiProps;
    private final ObjectMapper              mapper;

    // ── Consultas ──────────────────────────────────────────────────────

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

    /** Admin: lista los intentos IA de un estudiante. */
    public PagedResponse<AiTriedDto> findByStudent(String programId, String studentId, Pageable pageable) {
        return PagedResponse.from(aiTriedRepository
                .findByStudentIdAndProgramId(studentId, programId, pageable)
                .map(AiTriedDto::from));
    }

    /** Admin: ve las preguntas que la IA generó en un intento (con la correcta revelada). */
    public List<AiQuestionDto> questionsForAdmin(String aiTriedId) {
        AiTried at = getOrThrow(aiTriedId);
        return aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId)
                .stream().map(q -> toDto(q, at.getTotalQuestions(), true)).toList();
    }

    public List<AiQuestionDto> listQuestions(String aiTriedId, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        return aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId)
                .stream().map(q -> toDto(q, at.getTotalQuestions(), false)).toList();
    }

    public List<AiQuestionDto> getReview(String aiTriedId, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        return aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId)
                .stream().map(q -> toDto(q, at.getTotalQuestions(), true)).toList();
    }

    // ── Estado ──────────────────────────────────────────────────────────

    public AiStatusDto getStatus() {
        boolean enabled  = aiProps.isEnabled();
        String provider  = aiProps.getProvider().name();
        String model     = aiProps.getCloudModel();
        boolean reachable = false;
        String message;
        if (!enabled || aiProps.getProvider() == AiModelProperties.Provider.NONE) {
            message = "Módulo IA no habilitado.";
        } else {
            try {
                reachable = questionGenerator.isAvailable();
                message   = reachable ? "Servicio de preguntas activo" : "Proveedor configurado pero no alcanzable.";
            } catch (Exception e) { message = "Error verificando IA: " + e.getMessage(); }
        }
        return new AiStatusDto(enabled, provider, model, reachable, message);
    }

    // ── Iniciar - genera SÓLO la primera pregunta (adaptativo) ──────────

    @Transactional
    public AiStartResponseDto start(StartAiTriedRequest req, UserPrincipal p) {
        if (!questionGenerator.isAvailable())
            throw new ApiException(ErrorCode.AI_MODULE_DISABLED);

        Competence comp = competenceRepository.findById(req.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        // IRT acumulativo: arrancamos desde el theta que el usuario trae en esta competencia.
        double priorTheta = loadPriorTheta(p.getProgramId(), p.getId(), comp.getCompetenceId());

        AiTried at = AiTried.builder()
                .programId(p.getProgramId())
                .studentId(p.getId())
                .aiTriedId(IdGenerator.aiTriedId(aiTriedRepository.count()))
                .competenceId(comp.getCompetenceId())
                .totalQuestions(req.getTotalQuestions())
                .description(req.getDescription())
                .theta(BigDecimal.valueOf(priorTheta).setScale(3, RoundingMode.HALF_UP))
                .build();
        aiTriedRepository.save(at);

        // Primera pregunta en el nivel que corresponde al theta acumulado del usuario
        AiQuestion first = generateAndSave(at, comp, 1, thetaToDifficulty(priorTheta));
        return new AiStartResponseDto(AiTriedDto.from(at), toDto(first, req.getTotalQuestions(), false), 1);
    }

    // ── Generar siguiente pregunta adaptativa ────────────────────────────

    @Transactional
    public AiQuestionDto generateNextQuestion(String aiTriedId, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        assertInProgress(at);

        List<AiQuestion> existing = aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId);
        int nextOrder = existing.size() + 1;

        if (nextOrder > at.getTotalQuestions())
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED, "Ya se generaron todas las preguntas.");

        // Calcular theta actualizado (sembrado con el acumulado del usuario) y dificultad objetivo
        double prior    = loadPriorTheta(at.getProgramId(), at.getStudentId(), at.getCompetenceId());
        double theta    = computeTheta(existing, prior);
        int    target   = thetaToDifficulty(theta);

        Competence comp = competenceRepository.findById(at.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", at.getCompetenceId()));

        AiQuestion q = generateAndSave(at, comp, nextOrder, target);

        // Persistir theta actualizado
        at.setTheta(BigDecimal.valueOf(theta).setScale(3, RoundingMode.HALF_UP));
        aiTriedRepository.save(at);

        return toDto(q, at.getTotalQuestions(), false);
    }

    // ── Responder ────────────────────────────────────────────────────────

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

        q.setSelectedIndex(sel);
        q.setIsCorrect(isCorrect);
        q.setAnsweredAt(LocalDateTime.now());
        aiQuestionRepository.save(q);

        // Persistir en ai_tried_responses
        Competence comp = competenceRepository.findById(q.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", q.getCompetenceId()));
        aiTriedResponseRepository.save(AiTriedResponse.builder()
                .programId(at.getProgramId())
                .studentId(at.getStudentId())
                .aiTriedId(aiTriedId)
                .aiTriedResponseId(IdGenerator.aiTriedResponseId(aiTriedResponseRepository.count()))
                .questionText(truncate(q.getStatement(), 1990))
                .studentAnswer(truncate(options.get(sel).text(), 290))
                .correctAnswer(truncate(options.get(q.getCorrectIndex()).text(), 290))
                .isCorrect(isCorrect)
                .answeredAt(LocalDateTime.now())
                .competence(comp)
                .build());

        // Recalcular stats con scoring por dificultad
        List<AiQuestion> all = aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId);
        long answered   = all.stream().filter(x -> x.getSelectedIndex() != null).count();
        int[] pts       = computePoints(all);
        int earnedPts   = pts[0], maxPts = pts[1];
        int correctCount= (int) all.stream().filter(x -> Boolean.TRUE.equals(x.getIsCorrect())).count();

        at.setCorrectAnswers(correctCount);

        boolean finished = answered >= at.getTotalQuestions();
        boolean hasNext  = !finished;
        Double finalScore = null;
        int nextDiff = 0;

        double prior = loadPriorTheta(at.getProgramId(), at.getStudentId(), at.getCompetenceId());
        if (finished) {
            finishAiTried(at, all);
            finalScore = at.getScore() != null ? at.getScore().doubleValue() : 0.0;
        } else {
            double theta = computeTheta(all, prior);
            nextDiff = thetaToDifficulty(theta);
            at.setTheta(BigDecimal.valueOf(theta).setScale(3, RoundingMode.HALF_UP));
        }
        aiTriedRepository.save(at);

        return new AiAnswerResultDto(
                isCorrect, sel, q.getCorrectIndex(), q.getExplanation(),
                correctCount, (int) answered, at.getTotalQuestions(),
                finished, hasNext, finalScore,
                earnedPts, maxPts, nextDiff);
    }

    // ── Pista ────────────────────────────────────────────────────────────

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

        if (req.getHintLevel() > q.getHintsUsed() + 1)
            throw new ApiException(ErrorCode.AI_INVALID_OPTION, "Solicita las pistas en orden.");
        if (q.getHintsUsed() >= 3)
            throw new ApiException(ErrorCode.HINT_LIMIT_REACHED);

        List<AiOption> options = readOptions(q.getOptionsJson());
        List<String>   texts   = options.stream().map(AiOption::text).toList();

        String hint;
        try {
            hint = aiTutor.generateHint(new AiHintContext(
                    q.getCompetenceId(), q.getStatement(), texts,
                    options.get(q.getCorrectIndex()).text(), req.getHintLevel()));
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED, e.getMessage());
        }

        q.setHintsUsed(Math.max(q.getHintsUsed(), req.getHintLevel()));
        aiQuestionRepository.save(q);
        return new AiHintDto(clean(hint), req.getHintLevel());
    }

    // ── Finalizar manual ─────────────────────────────────────────────────

    @Transactional
    public AiTriedDto finish(String aiTriedId, Integer timeSpentSeconds, UserPrincipal p) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, p);
        if (!"IN_PROGRESS".equals(at.getStatus()))
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED);
        if (timeSpentSeconds != null) at.setTimeSpentSeconds(timeSpentSeconds);
        List<AiQuestion> all = aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId);
        finishAiTried(at, all);
        return AiTriedDto.from(aiTriedRepository.save(at));
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /** Genera UNA pregunta, la persiste y la retorna. */
    private AiQuestion generateAndSave(AiTried at, Competence comp, int order, int targetDiff) {
        List<AiGeneratedQuestion> batch;
        try {
            batch = questionGenerator.generateBatch(
                    comp.getCompetenceName(),
                    comp.getDescription() != null ? comp.getDescription() : "",
                    1, targetDiff);
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED, e.getMessage());
        }
        if (batch.isEmpty()) throw new ApiException(ErrorCode.AI_BATCH_EMPTY);

        AiGeneratedQuestion g = batch.get(0);
        List<AiOption> cleanedOptions = g.options().stream()
                .map(o -> new AiOption(clean(o.text()), o.isCorrect()))
                .toList();
        AiQuestion q = AiQuestion.builder()
                .aiQuestionId(IdGenerator.aiQuestionId(aiQuestionRepository.count()))
                .programId(at.getProgramId())
                .studentId(at.getStudentId())
                .aiTriedId(at.getAiTriedId())
                .competenceId(at.getCompetenceId())
                .questionOrder(order)
                .statement(clean(g.statement()))
                .optionsJson(writeOptions(cleanedOptions))
                .correctIndex(g.correctIndex())
                .explanation(clean(g.explanation()))
                .difficultyLevel(Math.max(1, Math.min(10, g.difficultyLevel())))
                .build();
        return aiQuestionRepository.save(q);
    }

    /** Normaliza el texto de la IA: reemplaza guiones largos (- – ―) por uno normal (-). */
    private static String clean(String s) {
        if (s == null) return null;
        return s.replace('-', '-')   // - em dash
                .replace('–', '-')    // – en dash
                .replace('―', '-');   // ― horizontal bar
    }

    /** Score ponderado por el nivel de cada pregunta (justo) y normalizado a 0–100. */
    private void finishAiTried(AiTried at, List<AiQuestion> questions) {
        at.setStatus("FINISHED");
        at.setFinishedAt(LocalDateTime.now());

        int earned = 0, max = 0, correct = 0, answered = 0;
        for (AiQuestion q : questions) {
            int pts = q.getDifficultyLevel() != null ? q.getDifficultyLevel() : 5;
            max += pts;
            if (q.getSelectedIndex() != null) answered++;
            if (Boolean.TRUE.equals(q.getIsCorrect())) { earned += pts; correct++; }
        }
        at.setCorrectAnswers(correct);
        at.setScore(max > 0
                ? BigDecimal.valueOf(earned)
                .divide(BigDecimal.valueOf(max), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // IRT acumulativo: actualizar el theta del usuario en esta competencia
        double prior = loadPriorTheta(at.getProgramId(), at.getStudentId(), at.getCompetenceId());
        double finalTheta = computeTheta(questions, prior);
        at.setTheta(BigDecimal.valueOf(finalTheta).setScale(3, RoundingMode.HALF_UP));
        persistTheta(at, finalTheta, answered);
    }

    /** Lee el theta acumulado del usuario en la competencia (0.0 si no existe). */
    private double loadPriorTheta(String programId, String studentId, String competenceId) {
        return aiUserCompetenceRepository
                .findById(new AiUserCompetenceId(programId, studentId, competenceId))
                .map(u -> u.getTheta().doubleValue())
                .orElse(0.0);
    }

    /** Upsert del theta acumulado del usuario tras finalizar un intento. */
    private void persistTheta(AiTried at, double theta, int answeredDelta) {
        AiUserCompetenceId id = new AiUserCompetenceId(
                at.getProgramId(), at.getStudentId(), at.getCompetenceId());
        AiUserCompetence u = aiUserCompetenceRepository.findById(id).orElseGet(() ->
                AiUserCompetence.builder()
                        .programId(at.getProgramId())
                        .studentId(at.getStudentId())
                        .competenceId(at.getCompetenceId())
                        .build());
        u.setTheta(BigDecimal.valueOf(Math.max(-3.0, Math.min(3.0, theta)))
                .setScale(3, RoundingMode.HALF_UP));
        u.setAnsweredTotal((u.getAnsweredTotal() == null ? 0 : u.getAnsweredTotal()) + Math.max(0, answeredDelta));
        aiUserCompetenceRepository.save(u);
    }

    /** Retorna [earnedPoints, maxPoints] para el set actual de preguntas. */
    private int[] computePoints(List<AiQuestion> questions) {
        int earned = 0, max = 0;
        for (AiQuestion q : questions) {
            int pts = q.getDifficultyLevel() != null ? q.getDifficultyLevel() : 5;
            max += pts;
            if (Boolean.TRUE.equals(q.getIsCorrect())) earned += pts;
        }
        return new int[]{earned, max};
    }

    /** Estimación theta con IRT 1PL simplificado, sembrada con el theta previo (acumulativo). */
    private double computeTheta(List<AiQuestion> questions, double seed) {
        double theta = Math.max(-3.0, Math.min(3.0, seed));
        for (AiQuestion q : questions) {
            if (q.getSelectedIndex() == null) continue;
            double b = (q.getDifficultyLevel() - 5.5) * (6.0 / 9.0);
            double p = 1.0 / (1.0 + Math.exp(b - theta));
            theta += Boolean.TRUE.equals(q.getIsCorrect())
                    ? 0.35 * (1.0 - p)
                    : -0.35 * p;
            theta = Math.max(-3.0, Math.min(3.0, theta));
        }
        return theta;
    }

    /** Convierte theta (-3..+3) a nivel de dificultad (1..10). */
    private int thetaToDifficulty(double theta) {
        int level = (int) Math.round(5.5 + theta * (9.0 / 6.0));
        return Math.max(1, Math.min(10, level));
    }

    private AiQuestionDto toDto(AiQuestion q, int total, boolean reveal) {
        List<AiOption> opts = readOptions(q.getOptionsJson());
        List<AiQuestionDto.OptionDto> dtoOpts = new ArrayList<>();
        for (int i = 0; i < opts.size(); i++)
            dtoOpts.add(new AiQuestionDto.OptionDto("OPT" + i, opts.get(i).text()));
        return new AiQuestionDto(
                q.getAiQuestionId(), q.getStatement(), dtoOpts, q.getDifficultyLevel(),
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
                .orElseThrow(() -> new ResourceNotFoundException("Sesión IA", id));
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