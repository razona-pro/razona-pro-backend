package com.razonapro.razonaprobackend.domain.question.repository;

import com.razonapro.razonaprobackend.domain.question.model.Question;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, QuestionId> {
    List<Question> findByCompetenceIdAndIsActiveTrue(String competenceId);
    Page<Question> findByCompetenceId(String competenceId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.competenceId = :competenceId")
    long countByCompetenceId(String competenceId);
}