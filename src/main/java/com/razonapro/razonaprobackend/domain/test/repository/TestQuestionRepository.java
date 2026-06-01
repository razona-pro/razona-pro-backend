package com.razonapro.razonaprobackend.domain.test.repository;

import com.razonapro.razonaprobackend.domain.test.model.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, Integer> {
    List<TestQuestion> findByTestIdAndCompetenceIdAndIsActiveTrue(String testId, String competenceId);
    boolean existsByCompetenceIdAndTestIdAndQuestionId(String competenceId, String testId, String questionId);
    Optional<TestQuestion> findByCompetenceIdAndTestIdAndQuestionId(String competenceId, String testId, String questionId);
    long countByTestIdAndCompetenceId(String testId, String competenceId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT tq.question.difficultyLevel, COUNT(tq)
        FROM TestQuestion tq
        WHERE tq.testId = :testId AND tq.competenceId = :competenceId AND tq.isActive = true
        GROUP BY tq.question.difficultyLevel
    """)
    List<Object[]> countByDifficulty(
        @org.springframework.data.repository.query.Param("testId")       String testId,
        @org.springframework.data.repository.query.Param("competenceId") String competenceId
    );
}