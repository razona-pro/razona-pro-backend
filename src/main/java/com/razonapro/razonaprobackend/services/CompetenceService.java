package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.CompetenceRequest;
import com.razonapro.razonaprobackend.dtos.response.CompetenceDto;
import com.razonapro.razonaprobackend.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.models.Competence;
import com.razonapro.razonaprobackend.repositories.CompetenceRepository;
import com.razonapro.razonaprobackend.util.IdGenerator;
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
        long count = competenceRepository.count();
        Competence c = Competence.builder()
            .competenceId(IdGenerator.competenceId(count))
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
