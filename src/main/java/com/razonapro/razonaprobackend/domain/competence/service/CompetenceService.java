package com.razonapro.razonaprobackend.domain.competence.service;

import com.razonapro.razonaprobackend.domain.competence.dto.request.CompetenceRequest;
import com.razonapro.razonaprobackend.domain.competence.dto.response.CompetenceDto;
import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetenceService {

    private final CompetenceRepository competenceRepository;

    public List<CompetenceDto> findAll() {
        return competenceRepository.findAll().stream().map(CompetenceDto::from).toList();
    }

    public List<CompetenceDto> findAllActive() {
        return competenceRepository.findByIsActiveTrue().stream().map(CompetenceDto::from).toList();
    }

    public CompetenceDto findById(String id) {
        return CompetenceDto.from(competenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", id)));
    }

    @Transactional
    public CompetenceDto create(CompetenceRequest req) {
        Competence c = Competence.builder()
                .competenceId(IdGenerator.competenceId(competenceRepository.count()))
                .competenceName(req.getCompetenceName())
                .description(req.getDescription())
                .build();
        return CompetenceDto.from(competenceRepository.save(c));
    }

    @Transactional
    public CompetenceDto update(String id, CompetenceRequest req) {
        Competence c = competenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", id));
        c.setCompetenceName(req.getCompetenceName());
        c.setDescription(req.getDescription());
        return CompetenceDto.from(competenceRepository.save(c));
    }

    @Transactional
    public void deactivate(String id) {
        Competence c = competenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competencia", id));
        c.setIsActive(false);
        competenceRepository.save(c);
    }
}