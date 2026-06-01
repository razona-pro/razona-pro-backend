package com.razonapro.razonaprobackend.domain.question.service;

import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.domain.question.dto.request.QuestionRequest;
import com.razonapro.razonaprobackend.domain.question.dto.request.QuestionUpdateRequest;
import com.razonapro.razonaprobackend.domain.question.dto.response.QuestionDto;
import com.razonapro.razonaprobackend.domain.question.model.Option;
import com.razonapro.razonaprobackend.domain.question.model.Question;
import com.razonapro.razonaprobackend.domain.question.repository.OptionRepository;
import com.razonapro.razonaprobackend.domain.question.repository.QuestionRepository;
import com.razonapro.razonaprobackend.infrastructure.security.UserPrincipal;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository   questionRepository;
    private final OptionRepository     optionRepository;
    private final CompetenceRepository competenceRepository;
    private final AdminRepository      adminRepository;

    public PagedResponse<QuestionDto> findByCompetence(String competenceId, Pageable pageable) {
        return PagedResponse.from(questionRepository.findByCompetenceId(competenceId, pageable)
                .map(q -> QuestionDto.from(q,
                        optionRepository.findByCompetenceIdAndQuestionId(q.getCompetenceId(), q.getQuestionId()))));
    }

    public PagedResponse<QuestionDto> findByFilters(
            String competenceId, String difficulty, String status, String search, Pageable pageable) {
        String comp = (competenceId != null && !competenceId.isBlank()) ? competenceId : null;
        String diff = (difficulty   != null && !difficulty.isBlank())   ? difficulty   : null;
        String srch = (search       != null && !search.isBlank())       ? search       : null;
        String sf   = (status       != null && !status.isBlank())       ? status.trim(): "";

        if (comp == null && diff == null && srch == null && sf.isEmpty()) {
            return PagedResponse.from(questionRepository.findAll(pageable)
                    .map(q -> QuestionDto.from(q,
                            optionRepository.findByCompetenceIdAndQuestionId(q.getCompetenceId(), q.getQuestionId()))));
        }
        return PagedResponse.from(questionRepository.findByFilters(comp, diff, srch, sf, pageable)
                .map(q -> QuestionDto.from(q,
                        optionRepository.findByCompetenceIdAndQuestionId(q.getCompetenceId(), q.getQuestionId()))));
    }

    public QuestionDto findById(String competenceId, String questionId) {
        Question q = questionRepository.findById(new QuestionId(competenceId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));
        return QuestionDto.from(q,
                optionRepository.findByCompetenceIdAndQuestionId(competenceId, questionId));
    }

    @Transactional
    public QuestionDto create(String competenceId, QuestionRequest req, UserPrincipal principal) {
        competenceRepository.findById(competenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", competenceId));

        boolean hasCorrect = req.getOptions().stream()
                .anyMatch(o -> Boolean.TRUE.equals(o.getIsCorrect()));
        if (!hasCorrect)
            throw new ApiException(ErrorCode.QUESTION_NO_CORRECT_OPTION);

        String questionId = IdGenerator.questionId(questionRepository.countByCompetenceId(competenceId));
        Question question = Question.builder()
                .competenceId(competenceId)
                .questionId(questionId)
                .admin(adminRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Admin", principal.getId())))
                .statement(req.getStatement())
                .explanation(req.getExplanation())
                .source(req.getSource())
                .difficultyLevel(req.getDifficultyLevel())
                .build();
        questionRepository.save(question);

        long optCount = 0;
        for (var optReq : req.getOptions()) {
            optionRepository.save(Option.builder()
                    .competenceId(competenceId)
                    .questionId(questionId)
                    .optionId(IdGenerator.optionId(optCount++))
                    .optionText(optReq.getOptionText())
                    .isCorrect(optReq.getIsCorrect())
                    .build());
        }

        return QuestionDto.from(question,
                optionRepository.findByCompetenceIdAndQuestionId(competenceId, questionId));
    }

    @Transactional
    public QuestionDto update(String competenceId, String questionId, QuestionUpdateRequest req) {
        Question q = questionRepository.findById(new QuestionId(competenceId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));
        if (req.getStatement()     != null) q.setStatement(req.getStatement());
        if (req.getDifficultyLevel()!= null) q.setDifficultyLevel(req.getDifficultyLevel());
        if (req.getSource()        != null) q.setSource(req.getSource());
        if (req.getExplanation()   != null) q.setExplanation(req.getExplanation());
        questionRepository.save(q);
        return QuestionDto.from(q, optionRepository.findByCompetenceIdAndQuestionId(competenceId, questionId));
    }

    @Transactional
    public void activate(String competenceId, String questionId) {
        Question q = questionRepository.findById(new QuestionId(competenceId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));
        q.setIsActive(true);
        questionRepository.save(q);
    }

    @Transactional
    public void deactivate(String competenceId, String questionId) {
        Question q = questionRepository.findById(new QuestionId(competenceId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));
        q.setIsActive(false);
        questionRepository.save(q);
    }
}