package com.razonapro.razonaprobackend.domain.aitried.repository;

import com.razonapro.razonaprobackend.domain.aitried.model.AiUserCompetence;
import com.razonapro.razonaprobackend.shared.ids.AiUserCompetenceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUserCompetenceRepository
        extends JpaRepository<AiUserCompetence, AiUserCompetenceId> {
}
