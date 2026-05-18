package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.AiTriedResponse;
import com.razonapro.razonaprobackend.models.ids.AiTriedResponseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiTriedResponseRepository extends JpaRepository<AiTriedResponse, AiTriedResponseId> {
    List<AiTriedResponse> findByAiTriedId(String aiTriedId);
}
