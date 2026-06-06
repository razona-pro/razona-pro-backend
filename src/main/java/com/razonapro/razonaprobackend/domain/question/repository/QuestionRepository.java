package com.razonapro.razonaprobackend.domain.question.repository;

import com.razonapro.razonaprobackend.domain.question.model.Question;
import com.razonapro.razonaprobackend.shared.ids.QuestionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, QuestionId> {
    List<Question> findByCompetenceIdAndIsActiveTrue(String competenceId);
    Page<Question> findByCompetenceId(String competenceId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.competenceId = :competenceId")
    long countByCompetenceId(@Param("competenceId") String competenceId);

    long countByIsActiveTrue();

    @Query("SELECT q FROM Question q WHERE q.competenceId = :competenceId AND q.questionId = :questionId")
    java.util.Optional<Question> findByCompetenceIdAndQuestionId(
        @Param("competenceId") String competenceId, @Param("questionId") String questionId);

    /**
     * Filtros opcionales. statusFilter: "active", "inactive" o "" (sin filtro).
     */
    @Query("""
        SELECT q FROM Question q
        WHERE (:competenceId = '' OR q.competenceId = :competenceId)
          AND (:difficulty = ''
               OR (:difficulty = 'NONE' AND q.difficultyLevel IS NULL)
               OR q.difficultyLevel = :difficulty)
          AND (:search = '' OR LOWER(q.statement) LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:statusFilter = '' OR
               (:statusFilter = 'active'   AND q.isActive = true)  OR
               (:statusFilter = 'inactive' AND q.isActive = false))
        ORDER BY q.competenceId ASC, q.questionId ASC
    """)
    Page<Question> findByFilters(
        @Param("competenceId")  String competenceId,
        @Param("difficulty")    String difficulty,
        @Param("search")        String search,
        @Param("statusFilter")  String statusFilter,
        Pageable pageable
    );
}
