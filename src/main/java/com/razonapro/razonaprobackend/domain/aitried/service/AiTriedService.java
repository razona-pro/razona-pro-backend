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

    /** Admin: historial de TODAS las sesiones IA; studentId es filtro opcional. */
    public PagedResponse<AiTriedDto> findAllForAdmin(String studentId, Pageable pageable) {
        String sid = (studentId == null || studentId.isBlank()) ? null : studentId.trim();
        return PagedResponse.from(aiTriedRepository.findForAdmin(sid, pageable).map(AiTriedDto::from));
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

        // Normalizar y validar las competencias elegidas (1..N).
        List<String> compIds = cleanComps(req.getCompetenceIds());
        if (compIds.isEmpty())
            throw new ApiException(ErrorCode.INVALID_INPUT, "Selecciona al menos una competencia.");
        for (String cid : compIds)
            competenceRepository.findById(cid)
                    .orElseThrow(() -> new ResourceNotFoundException("Competencia", cid));

        String primary = compIds.get(0);
        Competence firstComp = competenceRepository.findById(primary)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", primary));

        // IRT acumulativo: arrancamos desde el theta promedio del usuario en esas competencias.
        double priorTheta = avgPriorTheta(p.getProgramId(), p.getId(), compIds);

        AiTried at = AiTried.builder()
                .programId(p.getProgramId())
                .studentId(p.getId())
                .aiTriedId(IdGenerator.aiTriedId(aiTriedRepository.count()))
                .competenceId(primary)
                .competenceIdsCsv(String.join(",", compIds))
                .totalQuestions(req.getTotalQuestions())
                .description(req.getDescription())
                .theta(BigDecimal.valueOf(priorTheta).setScale(3, RoundingMode.HALF_UP))
                .build();
        aiTriedRepository.save(at);

        // Primera pregunta (competencia índice 0) en el nivel inicial derivado del progreso.
        AiQuestion first = generateAndSave(at, firstComp, 1, startLevel(priorTheta));
        aiTriedRepository.save(at);   // persistir questionsGenerated / maxPossibleScore
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

        // Dificultad objetivo según la máquina de niveles (3 aciertos suben, 2 fallos bajan).
        List<String> compIds = parseComps(at);
        double prior  = avgPriorTheta(at.getProgramId(), at.getStudentId(), compIds);
        int    target = computeLevel(existing, startLevel(prior)).level();

        // Multi-competencia: se rota entre las competencias elegidas por orden de pregunta.
        String cid = compIds.get((nextOrder - 1) % compIds.size());
        Competence comp = competenceRepository.findById(cid)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", cid));

        AiQuestion q = generateAndSave(at, comp, nextOrder, target);

        // Persistir el nivel alcanzado como theta (para el progreso acumulado del usuario)
        at.setTheta(BigDecimal.valueOf(levelToTheta(target)).setScale(3, RoundingMode.HALF_UP));
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
        // Mismo instante para created_at y answered_at: si answered_at se calcula antes que
        // el @Builder.Default de created_at, queda answered_at < created_at y viola el CHECK.
        LocalDateTime nowTs = LocalDateTime.now();
        aiTriedResponseRepository.save(AiTriedResponse.builder()
                .programId(at.getProgramId())
                .studentId(at.getStudentId())
                .aiTriedId(aiTriedId)
                .aiTriedResponseId(IdGenerator.aiTriedResponseId(aiTriedResponseRepository.count()))
                .questionText(truncate(q.getStatement(), 1990))
                .studentAnswer(truncate(options.get(sel).text(), 290))
                .correctAnswer(truncate(options.get(q.getCorrectIndex()).text(), 290))
                .isCorrect(isCorrect)
                .createdAt(nowTs)
                .answeredAt(nowTs)
                .competence(comp)
                .build());

        // Recalcular stats con scoring por dificultad
        List<AiQuestion> all = aiQuestionRepository.findByAiTriedIdOrderByQuestionOrderAsc(aiTriedId);
        long answered   = all.stream().filter(x -> x.getSelectedIndex() != null).count();
        int[] pts       = computePoints(all);
        int earnedPts   = pts[0], maxPts = pts[1];
        int correctCount= (int) all.stream().filter(x -> Boolean.TRUE.equals(x.getIsCorrect())).count();

        at.setCorrectAnswers(correctCount);
        at.setAnsweredQuestions((int) answered);

        boolean finished = answered >= at.getTotalQuestions();
        boolean hasNext  = !finished;
        Double finalScore = null;
        int nextDiff = 0;

        double prior = avgPriorTheta(at.getProgramId(), at.getStudentId(), parseComps(at));
        if (finished) {
            finishAiTried(at, all);
            finalScore = at.getScore() != null ? at.getScore().doubleValue() : 0.0;
        } else {
            // Siguiente nivel según la máquina por niveles (smurfing).
            nextDiff = computeLevel(all, startLevel(prior)).level();
            at.setTheta(BigDecimal.valueOf(levelToTheta(nextDiff)).setScale(3, RoundingMode.HALF_UP));
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
        if (timeSpentSeconds != null && timeSpentSeconds > 0) at.setTimeSpentSeconds(timeSpentSeconds);
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
                .competenceId(comp.getCompetenceId())   // competencia REAL de esta pregunta (multi-competencia)
                .questionOrder(order)
                .statement(clean(g.statement()))
                .optionsJson(writeOptions(cleanedOptions))
                .correctIndex(g.correctIndex())
                .explanation(clean(g.explanation()))
                .difficultyLevel(Math.max(1, Math.min(10, g.difficultyLevel())))
                .build();
        AiQuestion saved = aiQuestionRepository.save(q);
        // Actualizar métricas de la sesión (sirven incluso si el intento se abandona).
        at.setQuestionsGenerated((at.getQuestionsGenerated() == null ? 0 : at.getQuestionsGenerated()) + 1);
        at.setMaxPossibleScore((at.getMaxPossibleScore() == null ? 0 : at.getMaxPossibleScore()) + saved.getDifficultyLevel());
        return saved;
    }

    /** Normaliza el texto de la IA: reemplaza guiones largos (- – ―) por uno normal (-). */
    private static String clean(String s) {
        if (s == null) return null;
        return s.replace('-', '-')   // - em dash
                .replace('–', '-')    // – en dash
                .replace('―', '-');   // ― horizontal bar
    }

    /**
     * Score ponderado por el nivel (1-10) de cada pregunta: puntos crudos (suma de los
     * niveles de las correctas), misma lógica de incentivo que el simulacro estático.
     * NO se normaliza a 100. Fuente de verdad única en Java (sin triggers de cálculo).
     */
    private void finishAiTried(AiTried at, List<AiQuestion> questions) {
        // El status pasa a FINISHED al FINAL (ver nota en TriedService.finishTried):
        // si se marcara aquí, el flush de persistTheta dispararía el trigger de ranking
        // con score aún nulo y el puntaje aparecería "una sesión tarde".
        int earned = 0, correct = 0, answered = 0, maxPts = 0;
        for (AiQuestion q : questions) {
            int pts = q.getDifficultyLevel() != null ? q.getDifficultyLevel() : 5;
            maxPts += pts;
            if (q.getSelectedIndex() != null) answered++;
            if (Boolean.TRUE.equals(q.getIsCorrect())) { earned += pts; correct++; }
        }

        // Lógica por niveles + recompensa por racha de subir de nivel (smurfing).
        double prior   = avgPriorTheta(at.getProgramId(), at.getStudentId(), parseComps(at));
        LevelState st  = computeLevel(questions, startLevel(prior));
        int bonus      = st.bonus();

        at.setCorrectAnswers(correct);
        at.setAnsweredQuestions(answered);
        at.setQuestionsGenerated(questions.size());
        at.setMaxPossibleScore(maxPts);
        // El puntaje final pondera por nivel y suma el bonus por rachas de ascenso.
        at.setScore(BigDecimal.valueOf(earned + bonus).setScale(2, RoundingMode.HALF_UP));

        // Progreso acumulado: persistir el nivel alcanzado como theta para CADA competencia
        // de la sesión (con el nº de preguntas respondidas de esa competencia).
        double finalTheta = levelToTheta(st.level());
        at.setTheta(BigDecimal.valueOf(finalTheta).setScale(3, RoundingMode.HALF_UP));
        for (String cid : parseComps(at)) {
            int answeredForComp = (int) questions.stream()
                    .filter(q -> cid.equals(q.getCompetenceId()) && q.getSelectedIndex() != null)
                    .count();
            persistTheta(at, cid, finalTheta, answeredForComp);
        }

        // Recién ahora (con score y theta ya calculados) pasamos a FINISHED: el save() del
        // llamador hace un único UPDATE con status + score juntos y el trigger
        // trg_update_ranking_on_ai_tried recalcula el ranking al instante.
        at.setStatus("FINISHED");
        at.setFinishedAt(LocalDateTime.now());
    }

    /** Lee el theta acumulado del usuario en la competencia (0.0 si no existe). */
    private double loadPriorTheta(String programId, String studentId, String competenceId) {
        return aiUserCompetenceRepository
                .findById(new AiUserCompetenceId(programId, studentId, competenceId))
                .map(u -> u.getTheta().doubleValue())
                .orElse(0.0);
    }

    /** Theta inicial promedio del usuario sobre el conjunto de competencias de la sesión. */
    private double avgPriorTheta(String programId, String studentId, List<String> competenceIds) {
        if (competenceIds == null || competenceIds.isEmpty()) return 0.0;
        double sum = 0;
        for (String cid : competenceIds) sum += loadPriorTheta(programId, studentId, cid);
        return sum / competenceIds.size();
    }

    /** Competencias de la sesión (desde el CSV; cae a la principal si está vacío). */
    private List<String> parseComps(AiTried at) {
        if (at.getCompetenceIdsCsv() != null && !at.getCompetenceIdsCsv().isBlank()) {
            List<String> out = new ArrayList<>();
            for (String s : at.getCompetenceIdsCsv().split(",")) {
                String c = s.trim();
                if (!c.isEmpty()) out.add(c);
            }
            if (!out.isEmpty()) return out;
        }
        return at.getCompetenceId() != null ? List.of(at.getCompetenceId()) : List.of();
    }

    /** Normaliza la lista de competencias: trim + UPPER, sin vacíos ni duplicados; conserva el orden. */
    private static List<String> cleanComps(List<String> raw) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        if (raw != null) {
            for (String c : raw) {
                if (c != null && !c.isBlank()) out.add(c.trim().toUpperCase());
            }
        }
        return new ArrayList<>(out);
    }

    /** Upsert del theta acumulado del usuario en UNA competencia tras finalizar un intento. */
    private void persistTheta(AiTried at, String competenceId, double theta, int answeredDelta) {
        AiUserCompetenceId id = new AiUserCompetenceId(
                at.getProgramId(), at.getStudentId(), competenceId);
        AiUserCompetence u = aiUserCompetenceRepository.findById(id).orElseGet(() ->
                AiUserCompetence.builder()
                        .programId(at.getProgramId())
                        .studentId(at.getStudentId())
                        .competenceId(competenceId)
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

    /** Resultado de reproducir la sesión con la máquina de niveles. */
    private record LevelState(int level, int correctStreak, int wrongStreak, int climbStreak, int bonus) {}

    /**
     * Lógica de niveles tipo "ranked" (smurfing):
     *  - 3 aciertos SEGUIDOS → sube un nivel (resetea la racha de aciertos).
     *  - 2 fallos SEGUIDOS   → baja un nivel (resetea la racha de fallos).
     *  - Tras bajar hay que volver a encadenar 3 aciertos para subir.
     *  - Subir de nivel en racha otorga puntos de bonificación crecientes (1, 2, 3, ...);
     *    un fallo rompe la racha de ascenso y reinicia la bonificación.
     * Reproduce solo las preguntas YA respondidas, en orden.
     */
    private LevelState computeLevel(List<AiQuestion> questions, int startLevel) {
        int level = Math.max(1, Math.min(10, startLevel));
        int correctStreak = 0, wrongStreak = 0, climbStreak = 0, bonus = 0;
        for (AiQuestion q : questions) {
            if (q.getSelectedIndex() == null) continue;
            if (Boolean.TRUE.equals(q.getIsCorrect())) {
                correctStreak++;
                wrongStreak = 0;
                if (correctStreak >= 3) {
                    correctStreak = 0;
                    if (level < 10) level++;
                    climbStreak++;
                    bonus += climbStreak;   // recompensa creciente por racha de ascenso
                }
            } else {
                wrongStreak++;
                correctStreak = 0;
                climbStreak = 0;            // romper la racha reinicia la bonificación
                if (wrongStreak >= 2) {
                    wrongStreak = 0;
                    if (level > 1) level--;
                }
            }
        }
        return new LevelState(level, correctStreak, wrongStreak, climbStreak, bonus);
    }

    /** Nivel inicial (1..10) del usuario a partir de su progreso acumulado (theta). */
    private int startLevel(double theta) {
        int level = (int) Math.round(5.5 + theta * (9.0 / 6.0));
        return Math.max(1, Math.min(10, level));
    }

    /** Inverso de startLevel: convierte un nivel (1..10) a theta (-3..+3) para persistir el progreso. */
    private double levelToTheta(int level) {
        return (Math.max(1, Math.min(10, level)) - 5.5) * (6.0 / 9.0);
    }

    private AiQuestionDto toDto(AiQuestion q, int total, boolean reveal) {
        List<AiOption> opts = readOptions(q.getOptionsJson());
        List<AiQuestionDto.OptionDto> dtoOpts = new ArrayList<>();
        for (int i = 0; i < opts.size(); i++)
            dtoOpts.add(new AiQuestionDto.OptionDto("OPT" + i, opts.get(i).text()));
        boolean answered = q.getSelectedIndex() != null;
        return new AiQuestionDto(
                q.getAiQuestionId(), q.getStatement(), dtoOpts, q.getDifficultyLevel(),
                q.getQuestionOrder(), total, q.getHintsUsed(),
                q.getSelectedIndex(),
                reveal ? q.getIsCorrect() : (answered ? q.getIsCorrect() : null),
                (reveal || answered) ? q.getCorrectIndex() : null,
                (reveal || answered) ? q.getExplanation()  : null,
                q.getCompetenceId());
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