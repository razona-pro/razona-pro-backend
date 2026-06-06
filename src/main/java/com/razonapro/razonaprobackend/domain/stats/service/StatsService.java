package com.razonapro.razonaprobackend.domain.stats.service;

import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.domain.doubt.repository.QuestionDoubtRepository;
import com.razonapro.razonaprobackend.domain.question.repository.QuestionRepository;
import com.razonapro.razonaprobackend.domain.stats.dto.*;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.domain.test.repository.TestRepository;
import com.razonapro.razonaprobackend.domain.tried.repository.StudentResponseRepository;
import com.razonapro.razonaprobackend.domain.tried.repository.TriedRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StudentRepository          studentRepository;
    private final TriedRepository            triedRepository;
    private final CompetenceRepository       competenceRepository;
    private final QuestionRepository         questionRepository;
    private final TestRepository             testRepository;
    private final StudentResponseRepository  studentResponseRepository;
    private final QuestionDoubtRepository    doubtRepository;
    private final AiTriedRepository          aiTriedRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "home-stats")
    public HomeStatsDto homeStats() {
        return HomeStatsDto.builder()
                .activeStudents(studentRepository.countByIsActiveTrue())
                .completedTrieds(triedRepository.countByStatus("FINISHED"))
                .activeCompetences(competenceRepository.countByIsActiveTrue())
                .satisfactionRate(triedRepository.satisfactionPercentage())
                .totalQuestions(questionRepository.count())
                .totalTests(testRepository.count())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminOverviewDto adminOverview() {
        long finishedTrieds = triedRepository.countByStatus("FINISHED");
        long inProgressTrieds = triedRepository.countByStatus("IN_PROGRESS");

        // "Puntaje promedio" como % de aciertos global (correctas/total), no como puntos crudos.
        var perf = triedRepository.findStudentPerformanceSummary();
        long sumCorrect = perf.stream().mapToLong(r -> ((Number) r[3]).longValue()).sum();
        long sumTotal   = perf.stream().mapToLong(r -> ((Number) r[4]).longValue()).sum();
        double avgScore = sumTotal > 0 ? (sumCorrect * 100.0 / sumTotal) : 0.0;

        return AdminOverviewDto.builder()
                .totalStudents(studentRepository.count())
                .activeStudents(studentRepository.countByIsActiveTrue())
                .inactiveStudents(studentRepository.countByIsActiveFalse())
                .totalQuestions(questionRepository.count())
                .activeQuestions(questionRepository.countByIsActiveTrue())
                .totalCompetences(competenceRepository.count())
                .activeCompetences(competenceRepository.countByIsActiveTrue())
                .totalTests(testRepository.count())
                .finishedTrieds(finishedTrieds)
                .inProgressTrieds(inProgressTrieds)
                .openDoubts(doubtRepository.countByStatus("OPEN"))
                .totalAiSessions(aiTriedRepository.count())
                .avgScore(avgScore)
                .satisfactionRate(triedRepository.satisfactionPercentage())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StudentPerformanceDto> studentPerformance() {
        return triedRepository.findStudentPerformanceSummary().stream()
                .map(r -> {
                    long total   = ((Number) r[4]).longValue();
                    long correct = ((Number) r[3]).longValue();
                    return StudentPerformanceDto.builder()
                            .studentId(    (String) r[0])
                            .totalTrieds(  ((Number) r[1]).longValue())
                            .avgScore(     ((Number) r[2]).doubleValue())
                            .totalCorrect( correct)
                            .totalQuestions(total)
                            .accuracyRate( total > 0 ? (correct * 100.0 / total) : 0.0)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionTrendDto> questionTrends(String competenceId) {
        boolean filtered = (competenceId != null && !competenceId.isBlank());

        if (filtered) {
            // Query: questionId, total, correct (3 cols)
            return studentResponseRepository.findQuestionTrendsByCompetence(competenceId)
                    .stream().map(r -> {
                        String qId     = (String) r[0];
                        long   total   = ((Number) r[1]).longValue();
                        long   correct = ((Number) r[2]).longValue();
                        String stmt    = questionRepository
                                .findByCompetenceIdAndQuestionId(competenceId, qId)
                                .map(q -> q.getStatement()).orElse(null);
                        return QuestionTrendDto.builder()
                                .questionId(qId).competenceId(competenceId)
                                .statement(stmt != null && stmt.length() > 120 ? stmt.substring(0, 120) + "…" : stmt)
                                .totalAnswers(total).correctAnswers(correct)
                                .errorRate(total > 0 ? ((total - correct) * 100.0 / total) : 0.0)
                                .build();
                    }).toList();
        } else {
            // Query: competenceId, questionId, total, correct (4 cols)
            return studentResponseRepository.findQuestionTrendsAll()
                    .stream().map(r -> {
                        String compId  = (String) r[0];
                        String qId     = (String) r[1];
                        long   total   = ((Number) r[2]).longValue();
                        long   correct = ((Number) r[3]).longValue();
                        String stmt    = questionRepository
                                .findByCompetenceIdAndQuestionId(compId, qId)
                                .map(q -> q.getStatement()).orElse(null);
                        return QuestionTrendDto.builder()
                                .questionId(qId).competenceId(compId)
                                .statement(stmt != null && stmt.length() > 120 ? stmt.substring(0, 120) + "…" : stmt)
                                .totalAnswers(total).correctAnswers(correct)
                                .errorRate(total > 0 ? ((total - correct) * 100.0 / total) : 0.0)
                                .build();
                    }).toList();
        }
    }
}
