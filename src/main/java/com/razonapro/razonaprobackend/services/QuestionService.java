package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.QuestionRequest;
import com.razonapro.razonaprobackend.dtos.response.PagedResponse;
import com.razonapro.razonaprobackend.dtos.response.QuestionDto;
import com.razonapro.razonaprobackend.exception.ApiException;
import com.razonapro.razonaprobackend.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.models.Option;
import com.razonapro.razonaprobackend.models.Question;
import com.razonapro.razonaprobackend.models.ids.QuestionId;
import com.razonapro.razonaprobackend.repositories.AdminRepository;
import com.razonapro.razonaprobackend.repositories.CompetenceRepository;
import com.razonapro.razonaprobackend.repositories.OptionRepository;
import com.razonapro.razonaprobackend.repositories.QuestionRepository;
import com.razonapro.razonaprobackend.security.UserPrincipal;
import com.razonapro.razonaprobackend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository  questionRepository;
    private final OptionRepository    optionRepository;
    private final CompetenceRepository competenceRepository;
    private final AdminRepository     adminRepository;

    public PagedResponse<QuestionDto> findByCompetence(String competenceId, Pageable pageable) {
        Page<QuestionDto> page = questionRepository.findByCompetenceId(competenceId, pageable)
            .map(q -> {
                List<Option> opts = optionRepository.findByCompetenceIdAndQuestionId(
                    q.getCompetenceId(), q.getQuestionId());
                return QuestionDto.from(q, opts);
            });
        return PagedResponse.from(page);
    }

    public QuestionDto findById(String competenceId, String questionId) {
        Question q = questionRepository.findById(new QuestionId(competenceId, questionId))
            .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));
        List<Option> opts = optionRepository.findByCompetenceIdAndQuestionId(competenceId, questionId);
        return QuestionDto.from(q, opts);
    }

    @Transactional
    public QuestionDto create(String competenceId, QuestionRequest req, UserPrincipal principal) {
        competenceRepository.findById(competenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Competencia", competenceId));

        boolean hasCorrect = req.getOptions().stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsCorrect()));
        if (!hasCorrect)
            throw new ApiException("Debe haber al menos una opción correcta");

        long qCount = questionRepository.countByCompetenceId(competenceId);
        String questionId = IdGenerator.questionId(qCount);

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
            String optionId = IdGenerator.optionId(optCount++);
            Option option = Option.builder()
                .competenceId(competenceId)
                .questionId(questionId)
                .optionId(optionId)
                .optionText(optReq.getOptionText())
                .isCorrect(optReq.getIsCorrect())
                .build();
            optionRepository.save(option);
        }

        List<Option> opts = optionRepository.findByCompetenceIdAndQuestionId(competenceId, questionId);
        return QuestionDto.from(question, opts);
    }

    @Transactional
    public void deactivate(String competenceId, String questionId) {
        Question q = questionRepository.findById(new QuestionId(competenceId, questionId))
            .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));
        q.setIsActive(false);
        questionRepository.save(q);
    }
}
