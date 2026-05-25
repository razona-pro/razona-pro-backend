package com.razonapro.razonaprobackend.domain.stats.service;

import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.domain.question.repository.QuestionRepository;
import com.razonapro.razonaprobackend.domain.stats.dto.HomeStatsDto;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.domain.test.repository.TestRepository;
import com.razonapro.razonaprobackend.domain.tried.repository.TriedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StudentRepository    studentRepository;
    private final TriedRepository      triedRepository;
    private final CompetenceRepository competenceRepository;
    private final QuestionRepository   questionRepository;
    private final TestRepository       testRepository;

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
}