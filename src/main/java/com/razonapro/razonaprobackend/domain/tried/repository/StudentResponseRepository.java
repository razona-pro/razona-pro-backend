package com.razonapro.razonaprobackend.domain.tried.repository;

import com.razonapro.razonaprobackend.domain.tried.model.StudentResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentResponseRepository extends JpaRepository<StudentResponse, String> {

    List<StudentResponse> findByTriedIdAndOptionIdIsNotNull(String triedId);

    Optional<StudentResponse> findByTriedIdAndQuestionId(String triedId, String questionId);

    /** Lock pesimista para evitar inserciones duplicadas por concurrencia. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM StudentResponse sr WHERE sr.triedId = :triedId AND sr.questionId = :questionId")
    Optional<StudentResponse> findByTriedIdAndQuestionIdForUpdate(
            @Param("triedId") String triedId,
            @Param("questionId") String questionId);

    boolean existsByTriedIdAndQuestionId(String triedId, String questionId);

    long countByTriedIdAndIsCorrectTrueAndOptionIdIsNotNull(String triedId);

    // findByTriedId sigue igual
    List<StudentResponse> findByTriedId(String triedId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT sr.questionId,
               COUNT(sr) as totalAnswers,
               SUM(CASE WHEN sr.isCorrect = true THEN 1 ELSE 0 END) as correctAnswers
        FROM StudentResponse sr
        WHERE sr.competenceId = :competenceId AND sr.optionId IS NOT NULL
        GROUP BY sr.questionId
        ORDER BY correctAnswers ASC
    """)
    List<Object[]> findQuestionTrendsByCompetence(@org.springframework.data.repository.query.Param("competenceId") String competenceId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT sr.competenceId,
               sr.questionId,
               COUNT(sr) as totalAnswers,
               SUM(CASE WHEN sr.isCorrect = true THEN 1 ELSE 0 END) as correctAnswers
        FROM StudentResponse sr
        WHERE sr.optionId IS NOT NULL
        GROUP BY sr.competenceId, sr.questionId
        ORDER BY totalAnswers DESC
    """)
    List<Object[]> findQuestionTrendsAll();
}