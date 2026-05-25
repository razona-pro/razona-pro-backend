package com.razonapro.razonaprobackend.domain.aitried.repository;

import com.razonapro.razonaprobackend.domain.aitried.model.AiTriedResponse;
import com.razonapro.razonaprobackend.shared.ids.AiTriedResponseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiTriedResponseRepository extends JpaRepository<AiTriedResponse, AiTriedResponseId> {
    List<AiTriedResponse> findByAiTriedId(String aiTriedId);
}
