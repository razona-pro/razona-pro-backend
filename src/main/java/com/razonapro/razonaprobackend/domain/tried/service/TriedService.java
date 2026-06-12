package com.razonapro.razonaprobackend.domain.tried.service;

import com.razonapro.razonaprobackend.domain.question.model.Option;
import com.razonapro.razonaprobackend.domain.question.model.Question;
import com.razonapro.razonaprobackend.domain.question.repository.OptionRepository;
import com.razonapro.razonaprobackend.domain.question.repository.QuestionRepository;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.domain.test.model.Test;
import com.razonapro.razonaprobackend.domain.test.model.TestQuestion;
import com.razonapro.razonaprobackend.domain.test.repository.TestQuestionRepository;
import com.razonapro.razonaprobackend.domain.test.repository.TestRepository;
import com.razonapro.razonaprobackend.domain.tried.dto.request.StartTriedRequest;
import com.razonapro.razonaprobackend.domain.tried.dto.request.SubmitAnswerRequest;
import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.notification.service.NotificationService;
import com.razonapro.razonaprobackend.domain.tried.dto.response.CompetenceBreakdownDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedEligibilityDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedResumeDto;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedReviewDto;
import com.razonapro.razonaprobackend.domain.tried.model.StudentResponse;
import com.razonapro.razonaprobackend.domain.tried.model.Tried;
import com.razonapro.razonaprobackend.domain.tried.repository.StudentResponseRepository;
import com.razonapro.razonaprobackend.domain.tried.repository.TriedRepository;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.shared.ids.OptionId;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import com.razonapro.razonaprobackend.shared.ids.TestPK;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriedService {

    private final TriedRepository           triedRepository;
    private final StudentResponseRepository responseRepository;
    private final TestRepository            testRepository;
    private final TestQuestionRepository    testQuestionRepository;
    private final OptionRepository          optionRepository;
    private final QuestionRepository        questionRepository;
    private final StudentRepository         studentRepository;
    private final NotificationService       notificationService;
    private final AdminRepository           adminRepository;
    private final com.razonapro.razonaprobackend.infrastructure.email.EmailService emailService;

    /** Nº de eventos sospechosos antes de anular el intento por fraude. */
    /** Nº de eventos de plagio que se toleran. Al alcanzarlo se anula el intento Y se desactiva la cuenta. */
    private static final int FRAUD_LIMIT = 2;

    // ── Consultas ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<TriedDto> findMyTrieds(UserPrincipal principal, Pageable pageable) {
        Page<TriedDto> page = triedRepository
                .findByStudentIdAndProgramId(principal.getId(), principal.getProgramId(), pageable)
                .map(TriedDto::from);
        return PagedResponse.from(page);
    }

    /** Historial admin: todos los intentos (todos los estudiantes); studentId/status son filtros opcionales. */
    @Transactional(readOnly = true)
    public PagedResponse<TriedDto> findAllForAdmin(String studentId, String status, Pageable pageable) {
        String sid = (studentId == null || studentId.isBlank()) ? null : studentId.trim();
        String st  = (status    == null || status.isBlank())    ? null : status.trim();
        return PagedResponse.from(triedRepository.findForAdmin(sid, st, pageable).map(TriedDto::from));
    }

    @Transactional(readOnly = true)
    public TriedDto findById(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);
        return TriedDto.from(tried);
    }

    /**
     * Retorna el review completo de un intento FINISHED.
     * Incluye cada pregunta con opciones, cuál seleccionó el estudiante,
     * cuál era la correcta y la explicación.
     */
    @Transactional
    public TriedReviewDto getReview(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        Set<String> reviewable = Set.of("FINISHED", "ABANDONED", "ANULADO", "TIMED_OUT", "PLAGIO");
        if (!reviewable.contains(tried.getStatus())) {
            throw new ApiException(ErrorCode.TRIED_NOT_FINISHED);
        }

        // Retroalimentación de un solo uso para el estudiante: tras verla una vez (o salir),
        // ya no puede volver a abrirla. Los administradores la ven siempre.
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && Boolean.TRUE.equals(tried.getReviewViewed())) {
            throw new ApiException(ErrorCode.REVIEW_ALREADY_VIEWED);
        }

        // Cargar respuestas del estudiante indexadas por questionId.
        // Si existen duplicados por race condition, se conserva la más reciente.
        List<StudentResponse> responses = responseRepository.findByTriedId(triedId);
        Map<String, StudentResponse> responseByQuestion = responses.stream()
                .filter(r -> r.getOptionId() != null)
                .collect(Collectors.toMap(
                        StudentResponse::getQuestionId,
                        r -> r,
                        (a, b) -> {
                            if (b.getAnsweredAt() == null) return a;
                            if (a.getAnsweredAt() == null) return b;
                            return b.getAnsweredAt().isAfter(a.getAnsweredAt()) ? b : a;
                        }
                ));

        // Preguntas DEL INTENTO: se reconstruyen desde las respuestas del estudiante
        // (refleja exactamente lo presentado y es robusto si la prueba cambió después).
        // Si no hubiera respuestas, se cae a las preguntas asignadas al test.
        java.util.LinkedHashMap<String, String[]> presented = new java.util.LinkedHashMap<>();
        for (StudentResponse r : responses) {
            presented.putIfAbsent(r.getCompetenceId() + ":" + r.getQuestionId(),
                    new String[]{ r.getCompetenceId(), r.getQuestionId() });
        }
        if (presented.isEmpty()) {
            for (TestQuestion tq : testQuestionRepository.findByTestIdAndIsActiveTrue(tried.getTestId())) {
                presented.putIfAbsent(tq.getCompetenceId() + ":" + tq.getQuestionId(),
                        new String[]{ tq.getCompetenceId(), tq.getQuestionId() });
            }
        }

        List<TriedReviewDto.QuestionReview> questionReviews = new ArrayList<>();

        for (String[] pair : presented.values()) {
            String compId = pair[0], qId = pair[1];
            Question question = questionRepository
                    .findById(new QuestionId(compId, qId))
                    .orElse(null);
            if (question == null) continue;

            List<Option> options = optionRepository.findByCompetenceIdAndQuestionId(compId, qId);

            StudentResponse studentResp =
                    responseByQuestion.get(qId);
            String selectedOptionId = (studentResp != null) ? studentResp.getOptionId() : null;

            String correctOptionId = options.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .map(Option::getOptionId)
                    .findFirst()
                    .orElse(null);

            Boolean answeredCorrectly = (studentResp != null) ? studentResp.getIsCorrect() : false;

            List<TriedReviewDto.OptionReview> optionReviews = options.stream()
                    .map(o -> TriedReviewDto.OptionReview.builder()
                            .optionId(o.getOptionId())
                            .optionText(o.getOptionText())
                            .isCorrect(o.getIsCorrect())
                            .wasSelected(o.getOptionId().equals(selectedOptionId))
                            .build())
                    .toList();

            questionReviews.add(TriedReviewDto.QuestionReview.builder()
                    .questionId(question.getQuestionId())
                    .competenceId(compId)
                    .statement(question.getStatement())
                    .explanation(question.getExplanation())
                    .source(question.getSource())
                    .difficultyLevel(question.getDifficultyLevel())
                    .answeredCorrectly(answeredCorrectly)
                    .selectedOptionId(selectedOptionId)
                    .correctOptionId(correctOptionId)
                    .options(optionReviews)
                    .build());
        }

        // El estudiante consume su única visualización al abrir la retroalimentación.
        if (!isAdmin && !Boolean.TRUE.equals(tried.getReviewViewed())) {
            tried.setReviewViewed(true);
            triedRepository.save(tried);
        }

        return TriedReviewDto.builder()
                .triedId(tried.getTriedId())
                .testId(tried.getTestId())
                .testName(tried.getTest() != null ? tried.getTest().getTestName() : null)
                .status(tried.getStatus())
                .score(tried.getScore())
                .totalQuestions(tried.getTotalQuestions())
                .correctAnswers(tried.getCorrectAnswers())
                .timeSpentSeconds(tried.getTimeSpentSeconds())
                .attemptTimestamp(tried.getAttemptTimestamp())
                .finishedAt(tried.getFinishedAt())
                .questions(questionReviews)
                .build();
    }

    /**
     * Renuncia a la retroalimentación de un solo uso: si el estudiante sale de la pantalla
     * de resultados SIN abrir el review, lo marcamos como visto para que la URL directa ya
     * no lo muestre ("decidiste no ver tus respuestas"). Idempotente; admins no se afectan.
     */
    @Transactional
    public void forfeitReview(String triedId, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;
        Tried tried = triedRepository.findByTriedId(triedId).orElse(null);
        if (tried == null) return;
        // Solo el dueño puede renunciar a SU review.
        if (!tried.getStudentId().equals(principal.getId())) return;
        if (!Boolean.TRUE.equals(tried.getReviewViewed())) {
            tried.setReviewViewed(true);
            triedRepository.save(tried);
        }
    }

    /**
     * Desglose de aciertos por competencia de un intento finalizado, SIN revelar las
     * respuestas correctas ni las explicaciones. Lo puede ver el propio estudiante
     * (a diferencia del review completo, que es solo para administradores).
     */
    @Transactional(readOnly = true)
    public List<CompetenceBreakdownDto> getCompetenceBreakdown(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        Map<String, int[]> byComp = new LinkedHashMap<>();   // competenceId -> [correct, total]
        for (StudentResponse r : responseRepository.findByTriedId(triedId)) {
            if (r.getOptionId() == null) continue;            // no contar las sin responder
            int[] agg = byComp.computeIfAbsent(r.getCompetenceId(), k -> new int[2]);
            agg[1]++;
            if (Boolean.TRUE.equals(r.getIsCorrect())) agg[0]++;
        }
        return byComp.entrySet().stream()
                .map(e -> CompetenceBreakdownDto.builder()
                        .competenceId(e.getKey())
                        .correct(e.getValue()[0])
                        .total(e.getValue()[1])
                        .build())
                .toList();
    }

    // ── Elegibilidad: ¿puede el estudiante entrar a esta prueba? ──────────

    /**
     * Determina si el estudiante puede entrar a una prueba. Si hay un intento activo
     * (IN_PROGRESS) para esa prueba, se devuelve su id para REANUDARLO en vez de iniciar
     * uno nuevo. Los intentos son ilimitados, así que haber finalizado antes no bloquea.
     */
    @Transactional(readOnly = true)
    public TriedEligibilityDto checkEligibility(String testId, UserPrincipal principal) {
        Test test = testRepository.findByTestId(testId).orElse(null);
        if (test == null)
            return new TriedEligibilityDto(false, "La prueba no existe.", false, false, null);

        boolean active = Boolean.TRUE.equals(test.getIsActive());
        // Test-wide: cuenta las preguntas de todas las competencias asociadas a la prueba.
        int assignedCount = (int) testQuestionRepository.countByTestIdAndIsActiveTrue(testId);
        boolean hasQuestions = assignedCount > 0;
        // Mínimo a presentar: si questionsToPresent está definido, debe haber al menos esa cantidad asignada.
        Integer toPresent = test.getQuestionsToPresent();
        boolean meetsMinimum = (toPresent == null) ? hasQuestions : assignedCount >= toPresent;

        String inProgressTriedId = triedRepository
                .findInProgressByStudent(principal.getId(), principal.getProgramId())
                .stream()
                .filter(t -> t.getTestId().equals(testId))
                .map(Tried::getTriedId)
                .findFirst()
                .orElse(null);

        if (!active)
            return new TriedEligibilityDto(false, "Esta prueba no está disponible en este momento.",
                    false, hasQuestions, inProgressTriedId);
        if (!hasQuestions)
            return new TriedEligibilityDto(false, "Esta prueba aún no tiene preguntas asignadas.",
                    true, false, inProgressTriedId);
        if (!meetsMinimum)
            return new TriedEligibilityDto(false,
                    "La prueba requiere mostrar " + toPresent + " preguntas pero solo tiene "
                            + assignedCount + " asignada(s). No se puede iniciar hasta completar el banco.",
                    true, false, inProgressTriedId);

        // Activa y con suficientes preguntas: puede entrar (iniciar nuevo o reanudar el activo).
        return new TriedEligibilityDto(true, null, true, true, inProgressTriedId);
    }

    // ── Iniciar intento ──────────────────────────────────────────────────

    @Transactional
    public TriedDto startTried(StartTriedRequest req, UserPrincipal principal) {
        Test test = testRepository.findByTestId(req.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test", req.getTestId()));

        if (!Boolean.TRUE.equals(test.getIsActive()))
            throw new ApiException(ErrorCode.TEST_DISABLED);

        boolean alreadyInProgress = triedRepository
                .findInProgressByStudent(principal.getId(), principal.getProgramId())
                .stream()
                .anyMatch(t -> t.getTestId().equals(req.getTestId()));
        if (alreadyInProgress)
            throw new ApiException(ErrorCode.TRIED_IN_PROGRESS);

        List<TestQuestion> tqs = testQuestionRepository
                .findByTestIdAndIsActiveTrue(req.getTestId());
        if (tqs.isEmpty())
            throw new ApiException(ErrorCode.TEST_NO_QUESTIONS);

        // Validar mínimo: si el test pide presentar N preguntas, deben existir al menos N asignadas.
        Integer toPresent = test.getQuestionsToPresent();
        if (toPresent != null && tqs.size() < toPresent)
            throw new ApiException(ErrorCode.TEST_NO_QUESTIONS,
                    "La prueba requiere " + toPresent + " preguntas pero solo tiene "
                            + tqs.size() + " asignada(s).");

        // El id del intento se genera ANTES para sembrar la selección determinista:
        // así getTestQuestions(testId, triedId) y createUnansweredResponses eligen EXACTAMENTE
        // el mismo subconjunto que aquí (resiste recargas y evita inconsistencias).
        String newTriedId = IdGenerator.triedId(triedRepository.count());
        List<TestQuestion> selected = com.razonapro.razonaprobackend.domain.test.util.TestQuestionSelector
                .selectForTried(tqs, toPresent, newTriedId);

        Tried tried = Tried.builder()
                .testId(req.getTestId())
                .programId(principal.getProgramId())
                .studentId(principal.getId())
                .triedId(newTriedId)
                .totalQuestions(selected.size())
                .build();
        triedRepository.save(tried);

        // NO pre-crear StudentResponse - se crean al responder
        return TriedDto.fromWithQuestions(tried, selected);
    }
    /**
     * Garantiza que exista la opción centinela OTN000 para la pregunta.
     * Es la opción "Sin responder" - is_correct = false, nunca se muestra al estudiante.
     */
    private void ensureUnansweredOption(String competenceId, String questionId) {
        OptionId centinelaId = new OptionId(competenceId, questionId, IdGenerator.UNANSWERED_OPTION_ID);
        if (!optionRepository.existsById(centinelaId)) {
            optionRepository.save(Option.builder()
                    .competenceId(competenceId)
                    .questionId(questionId)
                    .optionId(IdGenerator.UNANSWERED_OPTION_ID)
                    .optionText("SIN RESPONDER")
                    .isCorrect(false)
                    .build());
        }
    }

    // ── Responder pregunta (UPDATE, no INSERT) ───────────────────────────

    @Transactional
    public TriedDto submitAnswer(String triedId, SubmitAnswerRequest req, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        if (!"IN_PROGRESS".equals(tried.getStatus()))
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED);

        if (IdGenerator.UNANSWERED_OPTION_ID.equals(req.getOptionId()))
            throw new ApiException(ErrorCode.INVALID_OPTION);

        // Resolver la competencia REAL de la pregunta dentro de esta prueba
        // (multi-competencia: puede diferir de la competencia principal del test).
        // El banco puede reutilizar el mismo question_id en varias competencias, así que
        // (test_id + question_id) NO es único: si el frontend envía competenceId lo usamos
        // de forma exacta; si no, tomamos la primera fila activa para no reventar.
        TestQuestion tq;
        if (StringUtils.hasText(req.getCompetenceId())) {
            tq = testQuestionRepository
                    .findByCompetenceIdAndTestIdAndQuestionId(
                            req.getCompetenceId(), tried.getTestId(), req.getQuestionId())
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT,
                            "Pregunta no pertenece a este test"));
        } else {
            tq = testQuestionRepository
                    .findAllByTestIdAndQuestionIdAndIsActiveTrue(tried.getTestId(), req.getQuestionId())
                    .stream().findFirst()
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT,
                            "Pregunta no pertenece a este test"));
        }
        String qComp = tq.getCompetenceId();

        Option selectedOption = optionRepository.findById(
                        new OptionId(qComp, req.getQuestionId(), req.getOptionId()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OPTION));

        // Buscar respuesta existente con lock para evitar inserciones duplicadas concurrentes
        var existingOpt = responseRepository.findByTriedIdAndQuestionIdForUpdate(triedId, req.getQuestionId());
        if (existingOpt.isPresent()) {
            StudentResponse sr = existingOpt.get();
            sr.setOptionId(req.getOptionId());
            sr.setIsCorrect(selectedOption.getIsCorrect());
            sr.setAnsweredAt(LocalDateTime.now());
            responseRepository.save(sr);
        } else {
            // created_at = answered_at (mismo instante) para no violar el CHECK answered_at >= created_at.
            LocalDateTime nowTs = LocalDateTime.now();
            responseRepository.save(StudentResponse.builder()
                    .studentResponseId(IdGenerator.studentResponseId(responseRepository.count()))
                    .competenceId(qComp)
                    .testId(tried.getTestId())
                    .programId(tried.getProgramId())
                    .studentId(tried.getStudentId())
                    .triedId(tried.getTriedId())
                    .questionId(req.getQuestionId())
                    .optionId(req.getOptionId())
                    .isCorrect(selectedOption.getIsCorrect())
                    .createdAt(nowTs)
                    .answeredAt(nowTs)
                    .build());
        }

        long correct = responseRepository.countByTriedIdAndIsCorrectTrueAndOptionIdIsNotNull(triedId);
        tried.setCorrectAnswers((int) correct);
        triedRepository.save(tried);

        return TriedDto.from(tried);
    }
    // ── Finalizar manualmente ────────────────────────────────────────────

    @Transactional
    public TriedDto finishManually(String triedId, Integer timeSpentSeconds, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        if (!"IN_PROGRESS".equals(tried.getStatus()))
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED);

        // El CHECK exige time_spent_seconds > 0; si llega 0/negativo lo dejamos en null.
        if (timeSpentSeconds != null && timeSpentSeconds > 0) tried.setTimeSpentSeconds(timeSpentSeconds);
        finishTried(tried);
        return TriedDto.from(triedRepository.save(tried));
    }

    // ── Reanudar intento (tiempo autoritativo del servidor) ──────────────

    /**
     * Reanuda un intento IN_PROGRESS. El tiempo SIEMPRE corre desde attempt_timestamp:
     * si ya expiró (test con duración), se marca ABANDONED. La práctica (sin duración)
     * nunca expira.
     */
    @Transactional
    public TriedResumeDto resume(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        Test test = testRepository.findByTestId(tried.getTestId())
                .orElse(null);
        Integer duration = (test != null) ? test.getDurationSeconds() : null;
        String  testMode = (test != null) ? test.getTestMode()        : null;

        if (!"IN_PROGRESS".equals(tried.getStatus()))
            return TriedResumeDto.of(tried, 0, duration, false, true, testMode, null);

        // Cargar respuestas previamente guardadas para que el frontend las restaure
        Map<String, String> savedAnswers = responseRepository.findByTriedId(triedId).stream()
                .filter(r -> r.getOptionId() != null)
                .collect(Collectors.toMap(
                        StudentResponse::getQuestionId,
                        StudentResponse::getOptionId,
                        (a, b) -> b,
                        LinkedHashMap::new));

        // Práctica sin tiempo: nunca expira
        if (duration == null)
            return TriedResumeDto.of(tried, null, null, false, false, testMode, savedAnswers);

        long elapsed   = java.time.Duration.between(tried.getAttemptTimestamp(), LocalDateTime.now()).getSeconds();
        long remaining = duration - elapsed;

        if (remaining <= 0) {
            tried.setStatus("ABANDONED");
            tried.setFinishedAt(LocalDateTime.now());
            tried.setTimeSpentSeconds((int) Math.min(elapsed, duration));
            createUnansweredResponses(tried);
            calculateScore(tried);
            triedRepository.save(tried);
            return TriedResumeDto.of(tried, 0, duration, true, true, testMode, null);
        }

        return TriedResumeDto.of(tried, (int) remaining, duration, false, false, testMode, savedAnswers);
    }

    // ── Fraude: registrar evento y anular si supera el límite ────────────

    /**
     * Registra un evento sospechoso (cambio de pestaña, salir de pantalla, etc.).
     * Se toleran hasta FRAUD_LIMIT eventos: al alcanzarlos, el intento queda en estado
     * PLAGIO, la cuenta del estudiante se DESACTIVA y se notifica a los admins.
     * No aplica en modo PRACTICE (la práctica es libre).
     */
    @Transactional
    public TriedDto reportFraud(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        if (!"IN_PROGRESS".equals(tried.getStatus()))
            return TriedDto.from(tried);

        Test test = testRepository.findByTestId(tried.getTestId())
                .orElse(null);
        if (test == null || "PRACTICE".equals(test.getTestMode()))
            return TriedDto.from(tried);  // la práctica no penaliza

        int count = (tried.getFraudAttempts() == null ? 0 : tried.getFraudAttempts()) + 1;
        tried.setFraudAttempts(count);

        if (count >= FRAUD_LIMIT) {
            // 1) Cambios CRÍTICOS que SÍ deben persistir: marcar el intento como PLAGIO
            //    y desactivar la cuenta del estudiante. Se guardan primero.
            tried.setStatus("PLAGIO");
            tried.setFinishedAt(LocalDateTime.now());
            // Plagio = intento ANULADO: no cuenta lo respondido, puntaje 0.
            tried.setScore(java.math.BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            tried.setCorrectAnswers(0);
            triedRepository.save(tried);
            deactivateStudentForFraud(principal);

            // 2) Notificaciones best-effort: corren en transacción propia (REQUIRES_NEW),
            //    así un fallo aquí NUNCA revierte la anulación ni la desactivación.
            try { notifyAdminsFraud(tried, principal); }
            catch (Exception e) { /* no romper el flujo de plagio por una notificación */ }
            try { notifyStudentDeactivated(principal); }
            catch (Exception e) { /* idem */ }
        }
        return TriedDto.from(triedRepository.save(tried));
    }

    /** Desactiva la cuenta del estudiante por plagio: deberá enviar una apelación para reactivarla. */
    private void deactivateStudentForFraud(UserPrincipal principal) {
        studentRepository.findByStudentId(principal.getId()).ifPresent(s -> {
            s.setIsActive(false);
            s.setDeactivationReason("FRAUD");
            studentRepository.save(s);
        });
    }

    /** Notifica al estudiante que su cuenta fue desactivada y cómo apelar. */
    private void notifyStudentDeactivated(UserPrincipal principal) {
        notificationService.notify(
                principal.getId(), "STUDENT", "ACCOUNT_DEACTIVATED",
                "Cuenta desactivada por plagio",
                "Tu cuenta fue desactivada tras detectar " + FRAUD_LIMIT + " eventos de plagio durante un examen. "
                        + "Para solicitar su reactivación debes enviar una apelación a un administrador.",
                "/dashboard/help");
    }

    private void notifyAdminsFraud(Tried tried, UserPrincipal principal) {
        String studentName = studentRepository.findByStudentId(principal.getId())
                .map(s -> (s.getFirstName() + " " + s.getFirstSurname()).trim())
                .orElse(principal.getId());
        String testName = (tried.getTest() != null) ? tried.getTest().getTestName() : tried.getTestId();
        final String sName = studentName, sId = principal.getId(), tName = testName;
        adminRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .forEach(a -> {
                    try {
                        notificationService.notify(
                            a.getAdminId(), "ADMIN", "FRAUD_ALERT",
                            "Intento anulado por plagio",
                            "El estudiante " + sName + " (" + sId + ") fue anulado por plagio en el test \""
                                    + tName + "\" tras " + tried.getFraudAttempts() + " eventos sospechosos.",
                            "/admin/appeals");
                    } catch (Exception ignored) { /* notificación no crítica */ }
                    try { emailService.sendFraudAdminEmail(a.getEmail(), a.getFirstName(), sName, sId, tName); }
                    catch (Exception ignored) { /* correo no crítico */ }
                });
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Score ponderado por dificultad. Fuente de verdad única: los triggers de cálculo
     * (trg_calculate_scores, trg_correct_answers) fueron eliminados, el cálculo vive aquí.
     * Pesos: dificultad N/A (NULL) = 1, Básico (B) = 1, Medio (M) = 2, Alto (A) = 3.
     * El score es la SUMA de pesos de las preguntas correctas (puntos crudos, NO sobre 100).
     */
    private void calculateScore(Tried tried) {
        List<StudentResponse> responses = responseRepository
                .findByTriedIdAndOptionIdIsNotNull(tried.getTriedId());
        int earned = 0, correct = 0;
        for (StudentResponse r : responses) {
            if (!Boolean.TRUE.equals(r.getIsCorrect())) continue;
            correct++;
            // Multi-competencia: el peso se busca con la competencia REAL de la respuesta.
            earned += questionRepository
                    .findByCompetenceIdAndQuestionId(r.getCompetenceId(), r.getQuestionId())
                    .map(q -> difficultyWeight(q.getDifficultyLevel()))
                    .orElse(1);
        }
        tried.setCorrectAnswers(correct);
        tried.setScore(BigDecimal.valueOf(earned).setScale(2, RoundingMode.HALF_UP));
    }

    /** Peso de dificultad para el puntaje: N/A o Básico = 1, Medio = 2, Alto = 3. */
    static int difficultyWeight(String difficultyLevel) {
        if (difficultyLevel == null) return 1;
        return switch (difficultyLevel) {
            case "M" -> 2;
            case "A" -> 3;
            default  -> 1;   // "B" o cualquier otro
        };
    }

    private void createUnansweredResponses(Tried tried) {
        // Solo las preguntas REALMENTE PRESENTADAS (mismo subconjunto determinista por intento
        // que vio el estudiante), no todo el banco. Cubre todas las competencias del subconjunto.
        List<TestQuestion> all = testQuestionRepository.findByTestIdAndIsActiveTrue(tried.getTestId());
        Integer toPresent = testRepository.findByTestId(tried.getTestId())
                .map(Test::getQuestionsToPresent).orElse(null);
        List<TestQuestion> testQuestions = com.razonapro.razonaprobackend.domain.test.util.TestQuestionSelector
                .selectForTried(all, toPresent, tried.getTriedId());
        long base = responseRepository.count();
        int idx = 0;
        for (TestQuestion tq : testQuestions) {
            if (!responseRepository.existsByTriedIdAndQuestionId(tried.getTriedId(), tq.getQuestionId())) {
                responseRepository.save(StudentResponse.builder()
                        .studentResponseId(IdGenerator.studentResponseId(base + idx))
                        .competenceId(tq.getCompetenceId())
                        .testId(tried.getTestId())
                        .programId(tried.getProgramId())
                        .studentId(tried.getStudentId())
                        .triedId(tried.getTriedId())
                        .questionId(tq.getQuestionId())
                        .optionId(null)
                        .isCorrect(false)
                        .answeredAt(null)
                        .build());
                idx++;
            }
        }
    }

    private void finishTried(Tried tried) {
        // IMPORTANTE: calcular el puntaje ANTES de pasar a FINISHED.
        // El trigger trg_update_ranking_on_tried dispara con AFTER UPDATE OF status y suma
        // solo trieds FINISHED con score NOT NULL. Si marcáramos FINISHED primero, el flush
        // que provoca createUnansweredResponses persistiría status=FINISHED con score aún nulo:
        // el trigger correría sin este intento y, como el UPDATE posterior solo cambia el score
        // (no el status), no volvería a dispararse → el puntaje aparecía "un test tarde".
        createUnansweredResponses(tried);
        calculateScore(tried);
        tried.setStatus("FINISHED");
        tried.setFinishedAt(LocalDateTime.now());
        // Con este orden, el save() del llamador hace UN solo UPDATE (status + score juntos)
        // y el trigger recalcula el ranking al instante con el puntaje correcto.
    }

    private void assertOwnership(Tried tried, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (!tried.getStudentId().equals(principal.getId())
                || !tried.getProgramId().equals(principal.getProgramId()))) {
            throw new ApiException(ErrorCode.INSUFFICIENT_PERMS);
        }
    }
}