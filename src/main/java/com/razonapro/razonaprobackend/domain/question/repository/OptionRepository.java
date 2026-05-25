package com.razonapro.razonaprobackend.domain.question.repository;

import com.razonapro.razonaprobackend.domain.question.model.Option;
import com.razonapro.razonaprobackend.shared.ids.OptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, OptionId> {
    List<Option> findByCompetenceIdAndQuestionId(String competenceId, String questionId);
    long countByCompetenceIdAndQuestionId(String competenceId, String questionId);
}