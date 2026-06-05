package com.razonapro.razonaprobackend.domain.test.service;

import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.domain.notification.service.NotificationService;
import com.razonapro.razonaprobackend.domain.question.dto.response.OptionDto;
import com.razonapro.razonaprobackend.domain.question.dto.response.QuestionDto;
import com.razonapro.razonaprobackend.domain.question.repository.OptionRepository;
import com.razonapro.razonaprobackend.domain.question.repository.QuestionRepository;
import com.razonapro.razonaprobackend.domain.test.dto.request.TestRequest;
import com.razonapro.razonaprobackend.domain.test.dto.request.TestUpdateRequest;
import com.razonapro.razonaprobackend.domain.test.dto.response.TestDto;
import com.razonapro.razonaprobackend.domain.test.model.Test;
import com.razonapro.razonaprobackend.domain.test.model.TestQuestion;
import com.razonapro.razonaprobackend.domain.test.repository.TestQuestionRepository;
import com.razonapro.razonaprobackend.domain.test.repository.TestRepository;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import com.razonapro.razonaprobackend.shared.ids.TestPK;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository         testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository     questionRepository;
    private final OptionRepository       optionRepository;
    private final CompetenceRepository   competenceRepository;
    private final AdminRepository        adminRepository;
    private final NotificationService    notificationService;

    @Transactional
    public TestDto activate(String testId, String competenceId) {
        Test test = testRepository.findById(new TestPK(testId, competenceId))
                .orElseThrow(() -> new ResourceNotFoundException("Test", testId));
        test.setIsActive(true);
        return TestDto.from(testRepository.save(test));
    }

    private Map<String, Long> getDifficultyBreakdown(String testId, String competenceId) {
        Map<String, Long> breakdown = new HashMap<>();
        testQuestionRepository.countByDifficulty(testId, competenceId)
                .forEach(r -> breakdown.put((String) r[0], (Long) r[1]));
        return breakdown;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TestDto> findAll(Pageable pageable, boolean activeOnly) {
        Page<Test> page = activeOnly
                ? testRepository.findAllActiveWithCompetence(pageable)
                : testRepository.findAllWithCompetence(pageable);
        return PagedResponse.from(page.map(t ->
                TestDto.from(t, getDifficultyBreakdown(t.getTestId(), t.getCompetenceId()))));
    }

    @Transactional(readOnly = true)
    public TestDto findById(String testId, String competenceId) {
        Test test = testRepository.findById(new TestPK(testId, competenceId))
                .orElseThrow(() -> new ResourceNotFoundException("Test", testId));
        return TestDto.from(test, getDifficultyBreakdown(testId, competenceId));
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> getTestQuestions(String testId, String competenceId, boolean showCorrect) {
        List<TestQuestion> tqs = testQuestionRepository
                .findByTestIdAndCompetenceIdAndIsActiveTrue(testId, competenceId);
        Test test = testRepository.findById(new TestPK(testId, competenceId))
                .orElseThrow(() -> new ResourceNotFoundException("Test", testId));

        List<TestQuestion> selected = tqs;
        if (test.getQuestionsToPresent() != null && test.getQuestionsToPresent() < tqs.size()) {
            selected = new ArrayList<>(tqs);
            Collections.shuffle(selected);
            selected = selected.subList(0, test.getQuestionsToPresent());
        }

        return selected.stream().map(tq -> {
            var q = questionRepository.findById(new QuestionId(tq.getCompetenceId(), tq.getQuestionId())).orElseThrow();
            var opts = optionRepository.findByCompetenceIdAndQuestionId(tq.getCompetenceId(), tq.getQuestionId());
            Collections.shuffle(opts);
            if (showCorrect) return QuestionDto.from(q, opts);
            return QuestionDto.builder()
                    .competenceId(q.getCompetenceId())
                    .questionId(q.getQuestionId())
                    .statement(q.getStatement())
                    .difficultyLevel(q.getDifficultyLevel())
                    .options(opts.stream().map(OptionDto::fromMasked).toList())
                    .build();
        }).toList();
    }

    @Transactional
    public TestDto create(TestRequest req, UserPrincipal principal) {
        competenceRepository.findById(req.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        String normalizedName = req.getTestName().trim();
        if (testRepository.existsByTestNameIgnoreCaseAndCompetenceId(normalizedName, req.getCompetenceId())) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE,
                    "Ya existe un test con el nombre \"" + req.getTestName() + "\" en esta competencia.");
        }

        // EXAM y TIMED requieren tiempo; PRACTICE puede ser sin tiempo (null)
        if (!"PRACTICE".equals(req.getTestMode()) && req.getDurationSeconds() == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT,
                    "Los modos EXAM y TIMED requieren una duración.");
        }

        Test test = Test.builder()
                .testId(IdGenerator.testId(testRepository.count()))
                .competenceId(req.getCompetenceId())
                .admin(adminRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Admin", principal.getId())))
                .testName(req.getTestName())
                .description(req.getDescription())
                .durationSeconds(req.getDurationSeconds())
                .questionsToPresent(req.getQuestionsToPresent())
                .testMode(req.getTestMode())
                .build();

        Test saved = testRepository.save(test);

        // El admin decide si notifica a todos los estudiantes al publicar el test
        if (!Boolean.FALSE.equals(req.getNotifyStudents())) {
            notificationService.broadcastNewTest(saved.getTestName(),
                    competenceRepository.findById(req.getCompetenceId())
                            .map(c -> c.getCompetenceName()).orElse(req.getCompetenceId()));
        }

        return TestDto.from(saved);
    }

    @Transactional
    public TestDto update(String testId, String competenceId, TestUpdateRequest req, UserPrincipal principal) {
        Test test = testRepository.findById(new TestPK(testId, competenceId))
                .orElseThrow(() -> new ResourceNotFoundException("Test", testId));

        if (StringUtils.hasText(req.getTestName())) {
            String normalizedName = req.getTestName().trim();
            if (testRepository.existsByTestNameIgnoreCaseAndCompetenceIdAndTestIdNot(
                    normalizedName, competenceId, testId)) {
                throw new ApiException(ErrorCode.DUPLICATE_RESOURCE,
                        "Ya existe un test con ese nombre en esta competencia.");
            }
            test.setTestName(req.getTestName());
        }
        if (StringUtils.hasText(req.getDescription()))  test.setDescription(req.getDescription());
        if (req.getDurationSeconds()    != null)       test.setDurationSeconds(req.getDurationSeconds());
        if (req.getQuestionsToPresent() != null)       test.setQuestionsToPresent(req.getQuestionsToPresent());
        if (StringUtils.hasText(req.getTestMode()))    test.setTestMode(req.getTestMode());
        if (req.getIsActive()           != null)       test.setIsActive(req.getIsActive());

        return TestDto.from(testRepository.save(test));
    }

    @Transactional
    public void addQuestion(String testId, String competenceId, String questionId, UserPrincipal principal) {
        testRepository.findById(new TestPK(testId, competenceId))
                .orElseThrow(() -> new ResourceNotFoundException("Test", testId));
        questionRepository.findById(new QuestionId(competenceId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));

        if (testQuestionRepository.existsByCompetenceIdAndTestIdAndQuestionId(competenceId, testId, questionId))
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "La pregunta ya está asignada a este test");

        long order = testQuestionRepository.countByTestIdAndCompetenceId(testId, competenceId) + 1;
        testQuestionRepository.save(TestQuestion.builder()
                .admin(adminRepository.findById(principal.getId()).orElseThrow())
                .competenceId(competenceId).testId(testId).questionId(questionId)
                .questionOrder((int) order).build());
    }

    @Transactional
    public void removeQuestion(String testId, String competenceId, String questionId) {
        TestQuestion tq = testQuestionRepository
                .findByCompetenceIdAndTestIdAndQuestionId(competenceId, testId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta en test", questionId));
        tq.setIsActive(false);
        testQuestionRepository.save(tq);
    }

    @Transactional
    public void deactivate(String testId, String competenceId) {
        Test test = testRepository.findById(new TestPK(testId, competenceId))
                .orElseThrow(() -> new ResourceNotFoundException("Test", testId));
        test.setIsActive(false);
        testRepository.save(test);
    }
}