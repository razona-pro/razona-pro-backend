// src/main/java/com/razonapro/razonaprobackend/domain/aitried/repository/AiQuestionRepository.java
package com.razonapro.razonaprobackend.domain.aitried.repository;

import com.razonapro.razonaprobackend.domain.aitried.model.AiQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiQuestionRepository extends JpaRepository<AiQuestion, String> {
    List<AiQuestion> findByAiTriedIdOrderByQuestionOrderAsc(String aiTriedId);
    Optional<AiQuestion> findByAiQuestionIdAndAiTriedId(String aiQuestionId, String aiTriedId);
    long countByAiTriedIdAndSelectedIndexIsNotNull(String aiTriedId);
}