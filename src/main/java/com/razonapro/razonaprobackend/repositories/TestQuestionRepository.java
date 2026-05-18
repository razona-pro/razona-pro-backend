package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.TestQuestion;
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
}
