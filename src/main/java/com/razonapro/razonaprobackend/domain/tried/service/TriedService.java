package com.razonapro.razonaprobackend.domain.tried.service;

import com.razonapro.razonaprobackend.domain.question.model.Option;
import com.razonapro.razonaprobackend.domain.question.repository.OptionRepository;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.domain.test.model.Test;
import com.razonapro.razonaprobackend.domain.test.repository.TestQuestionRepository;
import com.razonapro.razonaprobackend.domain.test.repository.TestRepository;
import com.razonapro.razonaprobackend.domain.tried.model.StudentResponse;
import com.razonapro.razonaprobackend.domain.tried.model.Tried;
import com.razonapro.razonaprobackend.domain.tried.repository.StudentResponseRepository;
import com.razonapro.razonaprobackend.domain.tried.repository.TriedRepository;
import com.razonapro.razonaprobackend.domain.tried.dto.request.StartTriedRequest;
import com.razonapro.razonaprobackend.domain.tried.dto.request.SubmitAnswerRequest;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.domain.tried.dto.response.TriedDto;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.shared.ids.OptionId;
import com.razonapro.razonaprobackend.shared.ids.TestPK;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
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
public class TriedService {

    private final TriedRepository triedRepository;
    private final StudentResponseRepository responseRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final OptionRepository optionRepository;
    private final StudentRepository studentRepository;

    public PagedResponse<TriedDto> findMyTrieds(UserPrincipal principal, Pageable pageable) {
        Page<TriedDto> page = triedRepository
            .findByStudentIdAndProgramId(principal.getId(), principal.getProgramId(), pageable)
            .map(TriedDto::from);
        return PagedResponse.from(page);
    }

    public TriedDto findById(String triedId, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
            .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);
        return TriedDto.from(tried);
    }

    @Transactional
    public TriedDto startTried(StartTriedRequest req, UserPrincipal principal) {
        Test test = testRepository.findById(new TestPK(req.getTestId(), req.getCompetenceId()))
            .orElseThrow(() -> new ResourceNotFoundException("Test", req.getTestId()));

        if (!test.getIsActive())
            throw new ApiException("El test no está disponible");

        // Verificar que no hay un intento en progreso para este test
        List<Tried> inProgress = triedRepository.findInProgressByStudent(
            principal.getId(), principal.getProgramId());
        boolean alreadyInProgress = inProgress.stream()
            .anyMatch(t -> t.getTestId().equals(req.getTestId()));
        if (alreadyInProgress)
            throw new ApiException("Ya tienes un intento en progreso para este test");

        long totalQuestions = testQuestionRepository
            .countByTestIdAndCompetenceId(req.getTestId(), req.getCompetenceId());
        int questionsCount = test.getQuestionsToPresent() != null
            ? Math.min(test.getQuestionsToPresent(), (int) totalQuestions)
            : (int) totalQuestions;

        if (questionsCount == 0)
            throw new ApiException("El test no tiene preguntas asignadas");

        Tried tried = Tried.builder()
            .competenceId(req.getCompetenceId())
            .testId(req.getTestId())
            .programId(principal.getProgramId())
            .studentId(principal.getId())
            .triedId(IdGenerator.triedId())
            .totalQuestions(questionsCount)
            .build();

        return TriedDto.from(triedRepository.save(tried));
    }

    @Transactional
    public TriedDto submitAnswer(String triedId, SubmitAnswerRequest req, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
            .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        if (!"IN_PROGRESS".equals(tried.getStatus()))
            throw new ApiException("El intento no está en progreso");

        if (responseRepository.existsByTriedIdAndQuestionId(triedId, req.getQuestionId()))
            throw new ApiException("Ya respondiste esta pregunta");

        // Validar que la opción existe y pertenece a la pregunta
        Option option = optionRepository.findById(
                new OptionId(tried.getCompetenceId(), req.getQuestionId(), req.getOptionId()))
            .orElseThrow(() -> new ApiException("Opción no válida para esta pregunta"));

        StudentResponse response = StudentResponse.builder()
            .studentResponseId(IdGenerator.studentResponseId())
            .competenceId(tried.getCompetenceId())
            .testId(tried.getTestId())
            .programId(tried.getProgramId())
            .studentId(tried.getStudentId())
            .triedId(triedId)
            .questionId(req.getQuestionId())
            .optionId(req.getOptionId())
            .isCorrect(option.getIsCorrect())
            .answeredAt(LocalDateTime.now())
            .build();
        responseRepository.save(response);

        // Actualizar contador de correctas
        long correct = responseRepository.countByTriedIdAndIsCorrectTrue(triedId);
        tried.setCorrectAnswers((int) correct);

        // Verificar si ya respondió todas las preguntas → auto-finish
        long answered = responseRepository.findByTriedId(triedId).size();
        if (answered >= tried.getTotalQuestions()) {
            finishTried(tried);
        }

        return TriedDto.from(triedRepository.save(tried));
    }

    @Transactional
    public TriedDto finishManually(String triedId, Integer timeSpentSeconds, UserPrincipal principal) {
        Tried tried = triedRepository.findByTriedId(triedId)
            .orElseThrow(() -> new ResourceNotFoundException("Intento", triedId));
        assertOwnership(tried, principal);

        if (!"IN_PROGRESS".equals(tried.getStatus()))
            throw new ApiException("El intento ya fue finalizado");

        if (timeSpentSeconds != null) tried.setTimeSpentSeconds(timeSpentSeconds);
        finishTried(tried);
        return TriedDto.from(triedRepository.save(tried));
    }

    private void finishTried(Tried tried) {
        tried.setStatus("FINISHED");
        tried.setFinishedAt(LocalDateTime.now());
        if (tried.getTotalQuestions() > 0 && tried.getCorrectAnswers() != null) {
            BigDecimal score = BigDecimal.valueOf(tried.getCorrectAnswers())
                .divide(BigDecimal.valueOf(tried.getTotalQuestions()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
            tried.setScore(score);
        }
    }

    private void assertOwnership(Tried tried, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (!tried.getStudentId().equals(principal.getId())
                || !tried.getProgramId().equals(principal.getProgramId()))) {
            throw new ApiException("No tienes acceso a este intento", HttpStatus.FORBIDDEN);
        }
    }
}
