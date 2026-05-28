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
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedDto;
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
import java.util.List;
import java.util.Map;
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

        if (!"FINISHED".equals(tried.getStatus())) {
            throw new ApiException(ErrorCode.TRIED_NOT_FINISHED);
        }

        // Cargar respuestas del estudiante indexadas por questionId
        List<StudentResponse> responses = responseRepository.findByTriedId(triedId);
        Map<String, StudentResponse> responseByQuestion = responses.stream()
                .filter(r -> r.getOptionId() != null)
                .collect(Collectors.toMap(StudentResponse::getQuestionId, r -> r));

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
                .triedId(IdGenerator.triedId())
                .totalQuestions(selected.size())
                .build();
        triedRepository.save(tried);

        // Pre-crear StudentResponse con opción centinela OTN000
        for (TestQuestion tq : selected) {
            // QUITA: ensureUnansweredOption(...)

            responseRepository.save(StudentResponse.builder()
                    .studentResponseId(IdGenerator.studentResponseId())
                    .competenceId(tried.getCompetenceId())
                    .testId(tried.getTestId())
                    .programId(tried.getProgramId())
                    .studentId(tried.getStudentId())
                    .triedId(tried.getTriedId())
                    .questionId(tq.getQuestionId())
                    .optionId(null)          // era: IdGenerator.UNANSWERED_OPTION_ID
                    .isCorrect(false)
                    .build());
        }

        return TriedDto.fromWithQuestions(tried, selected);
    }

    /**
     * Garantiza que exista la opción centinela OTN000 para la pregunta.
     * Es la opción "Sin responder" — is_correct = false, nunca se muestra al estudiante.
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

        // No permitir seleccionar la opción centinela
        if (IdGenerator.UNANSWERED_OPTION_ID.equals(req.getOptionId()))
            throw new ApiException(ErrorCode.INVALID_OPTION);

        StudentResponse sr = responseRepository
                .findByTriedIdAndQuestionId(triedId, req.getQuestionId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT,
                        "Pregunta no pertenece a este intento"));

        sr.setOptionId(req.getOptionId());
        sr.setIsCorrect(selectedOption.getIsCorrect());
        sr.setAnsweredAt(LocalDateTime.now());
        responseRepository.save(sr);

        // Contar correctas excluyendo la centinela
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

    // ── Helpers privados ─────────────────────────────────────────────────

    private void finishTried(Tried tried) {
        tried.setStatus("FINISHED");
        tried.setFinishedAt(LocalDateTime.now());

        // Solo respuestas reales (no la centinela OTN000)
        List<StudentResponse> responses = responseRepository
                .findByTriedIdAndOptionIdIsNotNull(tried.getTriedId());

        int totalPoints = 0, earnedPoints = 0;
        for (StudentResponse sr : responses) {
            Question q = questionRepository
                    .findById(new QuestionId(tried.getCompetenceId(), sr.getQuestionId()))
                    .orElse(null);
            int pts = (q != null) ? difficultyPoints(q.getDifficultyLevel()) : 3;
            totalPoints += pts;
            if (Boolean.TRUE.equals(sr.getIsCorrect())) earnedPoints += pts;
        }

        if (totalPoints > 0) {
            BigDecimal score = BigDecimal.valueOf(earnedPoints)
                    .divide(BigDecimal.valueOf(totalPoints), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            tried.setScore(score);
        } else {
            tried.setScore(BigDecimal.ZERO);
        }

        long correct = responses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        tried.setCorrectAnswers((int) correct);
    }

    private int difficultyPoints(String level) {
        return switch (level == null ? "M" : level) {
            case "B" -> 1;
            case "A" -> 5;
            default  -> 3;
        };
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