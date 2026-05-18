package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.TestRequest;
import com.razonapro.razonaprobackend.dtos.response.PagedResponse;
import com.razonapro.razonaprobackend.dtos.response.QuestionDto;
import com.razonapro.razonaprobackend.dtos.response.TestDto;
import com.razonapro.razonaprobackend.exception.ApiException;
import com.razonapro.razonaprobackend.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.models.Test;
import com.razonapro.razonaprobackend.models.TestQuestion;
import com.razonapro.razonaprobackend.models.ids.QuestionId;
import com.razonapro.razonaprobackend.models.ids.TestPK;
import com.razonapro.razonaprobackend.repositories.*;
import com.razonapro.razonaprobackend.security.UserPrincipal;
import com.razonapro.razonaprobackend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository         testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository     questionRepository;
    private final OptionRepository       optionRepository;
    private final CompetenceRepository   competenceRepository;
    private final AdminRepository        adminRepository;

    public PagedResponse<TestDto> findAll(Pageable pageable) {
        Page<TestDto> page = testRepository.findAll(pageable).map(TestDto::from);
        return PagedResponse.from(page);
    }

    public TestDto findById(String testId, String competenceId) {
        Test test = testRepository.findById(new TestPK(testId, competenceId))
            .orElseThrow(() -> new ResourceNotFoundException("Test", testId));
        return TestDto.from(test);
    }

    /** Devuelve las preguntas del test (enmascaradas para estudiante) */
    public List<QuestionDto> getTestQuestions(String testId, String competenceId, boolean showCorrect) {
        List<TestQuestion> tqs = testQuestionRepository
            .findByTestIdAndCompetenceIdAndIsActiveTrue(testId, competenceId);

        // Si el test tiene questions_to_present, selección aleatoria
        Test test = testRepository.findById(new TestPK(testId, competenceId))
            .orElseThrow(() -> new ResourceNotFoundException("Test", testId));

        List<TestQuestion> selected = tqs;
        if (test.getQuestionsToPresent() != null && test.getQuestionsToPresent() < tqs.size()) {
            selected = new java.util.ArrayList<>(tqs);
            Collections.shuffle(selected);
            selected = selected.subList(0, test.getQuestionsToPresent());
        }

        return selected.stream().map(tq -> {
            var q = questionRepository.findById(
                new QuestionId(tq.getCompetenceId(), tq.getQuestionId()))
                .orElseThrow();
            var opts = optionRepository.findByCompetenceIdAndQuestionId(
                tq.getCompetenceId(), tq.getQuestionId());
            if (showCorrect) return QuestionDto.from(q, opts);
            // Enmascarar respuesta correcta durante el examen
            var maskedOpts = opts.stream()
                .map(o -> com.razonapro.razonaprobackend.dtos.response.OptionDto.fromMasked(o))
                .toList();
            return QuestionDto.builder()
                .competenceId(q.getCompetenceId())
                .questionId(q.getQuestionId())
                .statement(q.getStatement())
                .difficultyLevel(q.getDifficultyLevel())
                .options(maskedOpts)
                .build();
        }).toList();
    }

    @Transactional
    public TestDto create(TestRequest req, UserPrincipal principal) {
        competenceRepository.findById(req.getCompetenceId())
            .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        long count = testRepository.count();
        Test test = Test.builder()
            .testId(IdGenerator.testId(count))
            .competenceId(req.getCompetenceId())
            .admin(adminRepository.findById(principal.getId()).orElseThrow())
            .testName(req.getTestName())
            .description(req.getDescription())
            .durationSeconds(req.getDurationSeconds())
            .questionsToPresent(req.getQuestionsToPresent())
            .testMode(req.getTestMode())
            .build();
        return TestDto.from(testRepository.save(test));
    }

    @Transactional
    public void addQuestion(String testId, String competenceId, String questionId, UserPrincipal principal) {
        testRepository.findById(new TestPK(testId, competenceId))
            .orElseThrow(() -> new ResourceNotFoundException("Test", testId));
        questionRepository.findById(new QuestionId(competenceId, questionId))
            .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));

        if (testQuestionRepository.existsByCompetenceIdAndTestIdAndQuestionId(competenceId, testId, questionId))
            throw new ApiException("La pregunta ya está asignada a este test");

        long order = testQuestionRepository.countByTestIdAndCompetenceId(testId, competenceId) + 1;
        TestQuestion tq = TestQuestion.builder()
            .admin(adminRepository.findById(principal.getId()).orElseThrow())
            .competenceId(competenceId)
            .testId(testId)
            .questionId(questionId)
            .questionOrder((int) order)
            .build();
        testQuestionRepository.save(tq);
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
