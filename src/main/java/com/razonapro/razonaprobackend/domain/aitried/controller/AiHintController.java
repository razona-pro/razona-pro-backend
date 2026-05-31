// src/main/java/com/razonapro/razonaprobackend/domain/aitried/controller/AiHintController.java
package com.razonapro.razonaprobackend.domain.aitried.controller;

import com.razonapro.razonaprobackend.domain.aitried.dto.response.AiHintDto;
import com.razonapro.razonaprobackend.domain.aitried.port.AiTutor;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.domain.question.repository.OptionRepository;
import com.razonapro.razonaprobackend.domain.question.repository.QuestionRepository;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/ai-hint")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
@Tag(name = "AI Hint", description = "Pistas IA para tests regulares")
public class AiHintController {

    private final AiTutor               aiTutor;
    private final QuestionRepository    questionRepository;
    private final OptionRepository      optionRepository;
    private final CompetenceRepository  competenceRepository;

    @GetMapping
    @Operation(summary = "Obtener pista IA para una pregunta del banco estático")
    public ResponseEntity<ApiResponse<AiHintDto>> getHint(
            @RequestParam @NotBlank String competenceId,
            @RequestParam @NotBlank String questionId,
            @RequestParam @Min(1) @Max(3) int hintLevel) {

        if (!aiTutor.isAvailable()) {
            throw new ApiException(ErrorCode.AI_TUTOR_DISABLED);
        }

        var comp = competenceRepository.findById(competenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", competenceId));

        var question = questionRepository
                .findById(new QuestionId(competenceId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));

        var options = optionRepository
                .findByCompetenceIdAndQuestionId(competenceId, questionId);

        String correctText = options.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .map(o -> o.getOptionText())
                .findFirst()
                .orElse("");

        var optionTexts = options.stream()
                .map(o -> o.getOptionText())
                .collect(Collectors.toList());

        AiHintContext ctx = new AiHintContext(
                comp.getCompetenceName(),
                question.getStatement(),
                optionTexts,
                correctText,
                hintLevel
        );

        String hint = aiTutor.generateHint(ctx);
        return ResponseEntity.ok(ApiResponse.ok(new AiHintDto(hint, hintLevel)));
    }
}