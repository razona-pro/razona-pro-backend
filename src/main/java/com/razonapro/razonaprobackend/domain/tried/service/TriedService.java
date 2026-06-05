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
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Nº de eventos sospechosos antes de anular el intento por fraude. */
    private static final int FRAUD_LIMIT = 3;

    // ── Consultas ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<TriedDto> findMyTrieds(UserPrincipal principal, Pageable pageable) {
        Page<TriedDto> page = triedRepository
                .findByStudentIdAndProgramId(principal.getId(), principal.getProgramId(), pageable)
                .map(TriedDto::from);
        return PagedResponse.from(page);
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
    @Transactional(readOnly = true)
    public TriedReviewDto getReview(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        Set<String> reviewable = Set.of("FINISHED", "ABANDONED", "ANULADO", "TIMED_OUT");
        if (!reviewable.contains(tried.getStatus())) {
            throw new ApiException(ErrorCode.TRIED_NOT_FINISHED);
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

        // Cargar las preguntas asignadas al test
        List<TestQuestion> testQuestions = testQuestionRepository
                .findByTestIdAndCompetenceIdAndIsActiveTrue(tried.getTestId(), tried.getCompetenceId());

        List<TriedReviewDto.QuestionReview> questionReviews = new ArrayList<>();

        for (TestQuestion tq : testQuestions) {
            Question question = questionRepository
                    .findById(new QuestionId(tq.getCompetenceId(), tq.getQuestionId()))
                    .orElse(null);
            if (question == null) continue;

            List<Option> options = optionRepository.findByCompetenceIdAndQuestionId(
                    tq.getCompetenceId(), tq.getQuestionId());

            StudentResponse studentResp =
                    responseByQuestion.get(tq.getQuestionId());
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

        return TriedReviewDto.builder()
                .triedId(tried.getTriedId())
                .testId(tried.getTestId())
                .testName(tried.getTest() != null ? tried.getTest().getTestName() : null)
                .competenceId(tried.getCompetenceId())
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

    // ── Iniciar intento ──────────────────────────────────────────────────

    @Transactional
    public TriedDto startTried(StartTriedRequest req, UserPrincipal principal) {
        Test test = testRepository.findById(new TestPK(req.getTestId(), req.getCompetenceId()))
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
                .findByTestIdAndCompetenceIdAndIsActiveTrue(req.getTestId(), req.getCompetenceId());
        if (tqs.isEmpty())
            throw new ApiException(ErrorCode.TEST_NO_QUESTIONS);

        List<TestQuestion> selected = new ArrayList<>(tqs);
        if (test.getQuestionsToPresent() != null && test.getQuestionsToPresent() < tqs.size()) {
            Collections.shuffle(selected);
            selected = selected.subList(0, test.getQuestionsToPresent());
        }

        Tried tried = Tried.builder()
                .competenceId(req.getCompetenceId())
                .testId(req.getTestId())
                .programId(principal.getProgramId())
                .studentId(principal.getId())
                .triedId(IdGenerator.triedId(triedRepository.count()))
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

        Option selectedOption = optionRepository.findById(
                        new OptionId(tried.getCompetenceId(), req.getQuestionId(), req.getOptionId()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OPTION));

        if (IdGenerator.UNANSWERED_OPTION_ID.equals(req.getOptionId()))
            throw new ApiException(ErrorCode.INVALID_OPTION);

        // Verificar que la pregunta pertenece al test
        boolean questionBelongs = testQuestionRepository
                .existsByCompetenceIdAndTestIdAndQuestionId(tried.getCompetenceId(), tried.getTestId(), req.getQuestionId());
        if (!questionBelongs)
            throw new ApiException(ErrorCode.INVALID_INPUT, "Pregunta no pertenece a este test");

        // Buscar respuesta existente con lock para evitar inserciones duplicadas concurrentes
        var existingOpt = responseRepository.findByTriedIdAndQuestionIdForUpdate(triedId, req.getQuestionId());
        if (existingOpt.isPresent()) {
            StudentResponse sr = existingOpt.get();
            sr.setOptionId(req.getOptionId());
            sr.setIsCorrect(selectedOption.getIsCorrect());
            sr.setAnsweredAt(LocalDateTime.now());
            responseRepository.save(sr);
        } else {
            responseRepository.save(StudentResponse.builder()
                    .studentResponseId(IdGenerator.studentResponseId(responseRepository.count()))
                    .competenceId(tried.getCompetenceId())
                    .testId(tried.getTestId())
                    .programId(tried.getProgramId())
                    .studentId(tried.getStudentId())
                    .triedId(tried.getTriedId())
                    .questionId(req.getQuestionId())
                    .optionId(req.getOptionId())
                    .isCorrect(selectedOption.getIsCorrect())
                    .answeredAt(LocalDateTime.now())
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

        if (timeSpentSeconds != null) tried.setTimeSpentSeconds(timeSpentSeconds);
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

        Test test = testRepository.findById(new TestPK(tried.getTestId(), tried.getCompetenceId()))
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
     * Al superar FRAUD_LIMIT, el intento se anula (ANULADO) y se notifica a los admins.
     * No aplica en modo PRACTICE (la práctica es libre).
     */
    @Transactional
    public TriedDto reportFraud(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        if (!"IN_PROGRESS".equals(tried.getStatus()))
            return TriedDto.from(tried);

        Test test = testRepository.findById(new TestPK(tried.getTestId(), tried.getCompetenceId()))
                .orElse(null);
        if (test == null || "PRACTICE".equals(test.getTestMode()))
            return TriedDto.from(tried);  // la práctica no penaliza

        int count = (tried.getFraudAttempts() == null ? 0 : tried.getFraudAttempts()) + 1;
        tried.setFraudAttempts(count);

        if (count >= FRAUD_LIMIT) {
            tried.setStatus("ANULADO");
            tried.setFinishedAt(LocalDateTime.now());
            createUnansweredResponses(tried);
            calculateScore(tried);
            notifyAdminsFraud(tried, principal);
        }
        return TriedDto.from(triedRepository.save(tried));
    }

    private void notifyAdminsFraud(Tried tried, UserPrincipal principal) {
        String studentName = studentRepository.findByStudentId(principal.getId())
                .map(s -> (s.getFirstName() + " " + s.getFirstSurname()).trim())
                .orElse(principal.getId());
        String testName = (tried.getTest() != null) ? tried.getTest().getTestName() : tried.getTestId();
        adminRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .forEach(a -> notificationService.notify(
                        a.getAdminId(), "ADMIN", "FRAUD_ALERT",
                        "Intento anulado por fraude",
                        "El estudiante " + studentName + " (" + principal.getId() + ") fue anulado por fraude en el test \""
                                + testName + "\" tras " + tried.getFraudAttempts() + " eventos sospechosos.",
                        "/admin/students"));
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private void calculateScore(Tried tried) {
        List<StudentResponse> responses = responseRepository
                .findByTriedIdAndOptionIdIsNotNull(tried.getTriedId());
        long correct = responses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        tried.setCorrectAnswers((int) correct);
        int total = tried.getTotalQuestions() != null ? tried.getTotalQuestions() : 0;
        tried.setScore(total > 0
                ? BigDecimal.valueOf(correct)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
    }

    private void createUnansweredResponses(Tried tried) {
        List<TestQuestion> testQuestions = testQuestionRepository
                .findByTestIdAndCompetenceIdAndIsActiveTrue(tried.getTestId(), tried.getCompetenceId());
        long base = responseRepository.count();
        int idx = 0;
        for (TestQuestion tq : testQuestions) {
            if (!responseRepository.existsByTriedIdAndQuestionId(tried.getTriedId(), tq.getQuestionId())) {
                responseRepository.save(StudentResponse.builder()
                        .studentResponseId(IdGenerator.studentResponseId(base + idx))
                        .competenceId(tried.getCompetenceId())
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
        tried.setStatus("FINISHED");
        tried.setFinishedAt(LocalDateTime.now());
        createUnansweredResponses(tried);
        calculateScore(tried);
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