package com.razonapro.razonaprobackend.domain.question.repository;

import com.razonapro.razonaprobackend.domain.question.model.Option;
import com.razonapro.razonaprobackend.shared.ids.OptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, OptionId> {
    List<Option> findByCompetenceIdAndQuestionId(String competenceId, String questionId);
    long countByCompetenceIdAndQuestionId(String competenceId, String questionId);

    /** DELETE directo por JPQL — evita SELECT+delete individual, seguro con triggers de auditoría. */
    @Modifying
    @Query("DELETE FROM Option o WHERE o.competenceId = :comp AND o.questionId = :qid")
    void deleteAllByQuestion(@Param("comp") String competenceId, @Param("qid") String questionId);
}