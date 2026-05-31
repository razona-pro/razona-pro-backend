// src/main/java/com/razonapro/razonaprobackend/domain/aitried/service/AiTriedService.java
package com.razonapro.razonaprobackend.domain.aitried.service;

import com.razonapro.razonaprobackend.domain.aitried.dto.request.AiHintRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.StartAiTriedRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.request.SubmitAiAnswerRequest;
import com.razonapro.razonaprobackend.domain.aitried.dto.response.*;
import com.razonapro.razonaprobackend.domain.aitried.model.AiTried;
import com.razonapro.razonaprobackend.domain.aitried.model.AiTriedResponse;
import com.razonapro.razonaprobackend.domain.aitried.port.AiQuestionGenerator;
import com.razonapro.razonaprobackend.domain.aitried.port.AiTutor;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.*;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedResponseRepository;
import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.infrastructure.ai.OllamaChatClient;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTriedService {

    private final AiTriedRepository         aiTriedRepository;
    private final AiTriedResponseRepository aiTriedResponseRepository;
    private final CompetenceRepository      competenceRepository;
    private final AiQuestionGenerator       questionGenerator;
    private final AiTutor                   aiTutor;
    private final AdaptiveEngine            adaptiveEngine;
    private final AiSessionStore            sessionStore;
    private final AiModelProperties         aiProps;

    // ── Consultas ─────────────────────────────────────────────────────

    public PagedResponse<AiTriedDto> findMy(UserPrincipal principal, Pageable pageable) {
        return PagedResponse.from(aiTriedRepository
                .findByStudentIdAndProgramId(principal.getId(), principal.getProgramId(), pageable)
                .map(AiTriedDto::from));
    }

    public AiTriedDto findById(String aiTriedId, UserPrincipal principal) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, principal);
        return AiTriedDto.from(at);
    }

    // ── Estado del módulo IA ──────────────────────────────────────────

    public AiStatusDto getStatus() {
        boolean enabled   = aiProps.isEnabled();
        String  provider  = aiProps.getProvider().name();
        String  model     = aiProps.getOllamaModel();
        boolean reachable = false;
        String  message;

        if (!enabled || aiProps.getProvider() == AiModelProperties.Provider.NONE) {
            message = "El módulo de IA está deshabilitado. Configura AI_MODEL_ENABLED=true y AI_MODEL_PROVIDER=OLLAMA.";
        } else if (aiProps.getProvider() == AiModelProperties.Provider.OLLAMA) {
            // Intentar ping a Ollama
            try {
                reachable = questionGenerator.isAvailable();
                message = reachable
                        ? "Ollama disponible. Modelo: " + model
                        : "Ollama configurado pero no alcanzable en " + aiProps.getOllamaBaseUrl();
            } catch (Exception e) {
                message = "Error verificando Ollama: " + e.getMessage();
            }
        } else {
            message = "Proveedor " + provider + " configurado (sin soporte de generación completa).";
        }

        return new AiStatusDto(enabled, provider, model, reachable, message);
    }

    // ── Iniciar sesión adaptativa ─────────────────────────────────────

    @Transactional
    public AiStartResponseDto start(StartAiTriedRequest req, UserPrincipal principal) {
        if (!questionGenerator.isAvailable()) {
            throw new ApiException(ErrorCode.AI_MODULE_DISABLED);
        }

        Competence comp = competenceRepository.findById(req.getCompetenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", req.getCompetenceId()));

        // Crear registro en BD
        AiTried at = AiTried.builder()
                .programId(principal.getProgramId())
                .studentId(principal.getId())
                .aiTriedId(IdGenerator.aiTriedId())
                .totalQuestions(req.getTotalQuestions())
                .description(req.getDescription())
                .build();
        aiTriedRepository.save(at);

        // Inicializar sesión en memoria
        AiSessionStore.SessionState state = new AiSessionStore.SessionState();
        state.competenceId          = comp.getCompetenceId();
        state.competenceName        = comp.getCompetenceName();
        state.competenceDescription = comp.getDescription() != null ? comp.getDescription() : "";
        sessionStore.put(at.getAiTriedId(), state);

        // Generar la primera pregunta
        AiGeneratedQuestion firstQ = generateQuestion(state);
        setCurrentQuestion(state, firstQ);

        return new AiStartResponseDto(AiTriedDto.from(at), toDto(firstQ, 1, req.getTotalQuestions()));
    }

    // ── Siguiente pregunta ────────────────────────────────────────────

    public AiQuestionDto nextQuestion(String aiTriedId, UserPrincipal principal) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, principal);
        assertInProgress(at);

        AiSessionStore.SessionState state = getSessionOrThrow(aiTriedId);

        // Idempotencia: si ya hay una pregunta pendiente, devolverla
        if (state.awaitingAnswer && state.currentQuestion != null) {
            return toDto(state.currentQuestion, state.servedCount, at.getTotalQuestions());
        }

        // Verificar que no se hayan acabado las preguntas
        if (state.servedCount >= at.getTotalQuestions()) {
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED,
                    "Ya respondiste todas las preguntas. Usa /finish para obtener tu puntaje final.");
        }

        // Generar siguiente pregunta
        AiGeneratedQuestion nextQ = generateQuestion(state);
        setCurrentQuestion(state, nextQ);

        return toDto(nextQ, state.servedCount, at.getTotalQuestions());
    }

    // ── Responder pregunta ────────────────────────────────────────────

    @Transactional
    public AiAnswerResultDto submitAnswer(String aiTriedId, SubmitAiAnswerRequest req,
                                          UserPrincipal principal) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, principal);
        assertInProgress(at);

        AiSessionStore.SessionState state = getSessionOrThrow(aiTriedId);

        if (state.currentQuestion == null || !state.awaitingAnswer) {
            throw new ApiException(ErrorCode.AI_QUESTION_NOT_MATCH,
                    "No hay una pregunta activa. Llama a /next primero.");
        }

        // Validar que el questionId coincida
        if (!state.currentQuestion.questionId().equals(req.getQuestionId())) {
            throw new ApiException(ErrorCode.AI_QUESTION_NOT_MATCH,
                    "La pregunta enviada no corresponde a la sesión activa.");
        }

        // Parsear opción seleccionada
        int selectedIdx = parseOptionId(req.getSelectedOptionId());
        if (selectedIdx < 0 || selectedIdx >= state.currentQuestion.options().size()) {
            throw new ApiException(ErrorCode.AI_INVALID_OPTION,
                    "Opción inválida: " + req.getSelectedOptionId());
        }

        AiGeneratedQuestion q = state.currentQuestion;
        boolean isCorrect = (selectedIdx == q.correctIndex());

        // Texto de las opciones
        String studentAnswerText = truncate(q.options().get(selectedIdx).text(), 290);
        String correctAnswerText = truncate(q.options().get(q.correctIndex()).text(), 290);

        // Persistir respuesta
        Competence comp = competenceRepository.findById(state.competenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", state.competenceId));

        AiTriedResponse response = AiTriedResponse.builder()
                .programId(at.getProgramId())
                .studentId(at.getStudentId())
                .aiTriedId(aiTriedId)
                .aiTriedResponseId(IdGenerator.aiTriedResponseId())
                .questionText(truncate(q.statement(), 1990))
                .studentAnswer(studentAnswerText)
                .correctAnswer(correctAnswerText)
                .isCorrect(isCorrect)
                .answeredAt(LocalDateTime.now())
                .build();
        response.setCompetence(comp);
        aiTriedResponseRepository.save(response);

        // Actualizar conteos
        long correctCount = aiTriedResponseRepository
                .findByAiTriedId(aiTriedId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        at.setCorrectAnswers((int) correctCount);

        // Actualizar θ
        state.theta = adaptiveEngine.updateTheta(state.theta, q.difficultyLevel(), isCorrect);
        state.awaitingAnswer = false;
        state.usedStatements.add(q.statement().substring(0, Math.min(80, q.statement().length())));

        // Verificar si se terminó la sesión
        boolean finished = state.servedCount >= at.getTotalQuestions();
        Double finalScore = null;

        if (finished) {
            finishAiTried(at);
            finalScore = at.getScore() != null ? at.getScore().doubleValue() : 0.0;
            aiTriedRepository.save(at);
            sessionStore.remove(aiTriedId);
            log.info("Sesión AI finalizada: {} — puntaje={}", aiTriedId, finalScore);
        } else {
            aiTriedRepository.save(at);
        }

        return new AiAnswerResultDto(
                isCorrect,
                req.getSelectedOptionId(),
                "OPT" + q.correctIndex(),
                q.explanation(),
                (int) correctCount,
                state.servedCount,
                at.getTotalQuestions(),
                finished,
                finalScore
        );
    }

    // ── Pista / Hint ─────────────────────────────────────────────────

    public AiHintDto getHint(String aiTriedId, AiHintRequest req, UserPrincipal principal) {
        if (!aiTutor.isAvailable()) {
            throw new ApiException(ErrorCode.AI_TUTOR_DISABLED);
        }

        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, principal);
        assertInProgress(at);

        AiSessionStore.SessionState state = getSessionOrThrow(aiTriedId);

        if (state.currentQuestion == null) {
            throw new ApiException(ErrorCode.AI_QUESTION_NOT_MATCH,
                    "No hay una pregunta activa para dar pista.");
        }

        if (!state.currentQuestion.questionId().equals(req.getQuestionId())) {
            throw new ApiException(ErrorCode.AI_QUESTION_NOT_MATCH,
                    "La pregunta de la pista no coincide con la pregunta activa.");
        }

        AiGeneratedQuestion q = state.currentQuestion;
        List<String> optionTexts = q.options().stream()
                .map(AiOption::text).collect(Collectors.toList());
        String correctText = q.options().get(q.correctIndex()).text();

        AiHintContext ctx = new AiHintContext(
                state.competenceName,
                q.statement(),
                optionTexts,
                correctText,
                req.getHintLevel()
        );

        String hint = aiTutor.generateHint(ctx);
        return new AiHintDto(hint, req.getHintLevel());
    }

    // ── Finalizar manualmente ─────────────────────────────────────────

    @Transactional
    public AiTriedDto finish(String aiTriedId, Integer timeSpentSeconds, UserPrincipal principal) {
        AiTried at = getOrThrow(aiTriedId);
        assertOwnership(at, principal);

        if (!"IN_PROGRESS".equals(at.getStatus())) {
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED);
        }

        if (timeSpentSeconds != null) at.setTimeSpentSeconds(timeSpentSeconds);
        finishAiTried(at);
        sessionStore.remove(aiTriedId);

        return AiTriedDto.from(aiTriedRepository.save(at));
    }

    // ── Helpers privados ──────────────────────────────────────────────

    private AiGeneratedQuestion generateQuestion(AiSessionStore.SessionState state) {
        int difficulty = adaptiveEngine.nextDifficulty(state.theta);

        // Tomar las últimas 5 para evitar repetición
        List<String> recentStatements = state.usedStatements.stream()
                .skip(Math.max(0, state.usedStatements.size() - 5))
                .collect(Collectors.toList());

        AiQuestionRequest req = new AiQuestionRequest(
                state.competenceId,
                state.competenceName,
                state.competenceDescription,
                difficulty,
                recentStatements
        );

        try {
            return questionGenerator.generate(req);
        } catch (OllamaChatClient.OllamaUnavailableException e) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("Error generando pregunta", e);
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED,
                    "No se pudo generar la pregunta: " + e.getMessage());
        }
    }

    private void setCurrentQuestion(AiSessionStore.SessionState state, AiGeneratedQuestion q) {
        state.currentQuestion = q;
        state.awaitingAnswer  = true;
        state.servedCount++;
    }

    private AiQuestionDto toDto(AiGeneratedQuestion q, int questionNumber, int total) {
        List<AiQuestionDto.OptionDto> opts = new ArrayList<>();
        for (int i = 0; i < q.options().size(); i++) {
            opts.add(new AiQuestionDto.OptionDto("OPT" + i, q.options().get(i).text()));
        }
        return new AiQuestionDto(
                q.questionId(),
                q.statement(),
                opts,
                q.difficultyLevel(),
                questionNumber,
                total
        );
    }

    private int parseOptionId(String optId) {
        if (optId == null) return -1;
        try {
            String num = optId.startsWith("OPT") ? optId.substring(3) : optId;
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return -1;
        }
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

    private AiTried getOrThrow(String aiTriedId) {
        return aiTriedRepository.findByAiTriedId(aiTriedId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Intento", aiTriedId));
    }

    private AiSessionStore.SessionState getSessionOrThrow(String aiTriedId) {
        return sessionStore.get(aiTriedId)
                .orElseThrow(() -> new ApiException(ErrorCode.AI_SESSION_NOT_FOUND,
                        "La sesión expiró o no existe. Por favor inicia una nueva práctica."));
    }

    private void assertOwnership(AiTried at, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (!at.getStudentId().equals(principal.getId())
                || !at.getProgramId().equals(principal.getProgramId()))) {
            throw new ApiException(ErrorCode.INSUFFICIENT_PERMS);
        }
    }

    private void assertInProgress(AiTried at) {
        if (!"IN_PROGRESS".equals(at.getStatus())) {
            throw new ApiException(ErrorCode.TRIED_ALREADY_FINISHED,
                    "Este intento ya está " + at.getStatus().toLowerCase() + ".");
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}