package com.razonapro.razonaprobackend.domain.program.service;

import com.razonapro.razonaprobackend.domain.program.dto.request.ProgramRequest;
import com.razonapro.razonaprobackend.domain.program.dto.response.ProgramDto;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.domain.program.model.Program;
import com.razonapro.razonaprobackend.domain.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;

    public List<ProgramDto> findAll() {
        return programRepository.findAll().stream().map(ProgramDto::from).toList();
    }

    public List<ProgramDto> findAllActive() {
        return programRepository.findByIsActiveTrue().stream().map(ProgramDto::from).toList();
    }

    public ProgramDto findById(String id) {
        return ProgramDto.from(programRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Programa", id)));
    }

    @Transactional
    public ProgramDto create(ProgramRequest req) {
        if (programRepository.existsById(req.getProgramId()))
            throw new ApiException("Ya existe un programa con el ID: " + req.getProgramId());
        Program p = Program.builder()
            .programId(req.getProgramId().toUpperCase())
            .programName(req.getProgramName())
            .description(req.getDescription())
            .build();
        return ProgramDto.from(programRepository.save(p));
    }

    @Transactional
    public ProgramDto update(String id, ProgramRequest req) {
        Program p = programRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Programa", id));
        p.setProgramName(req.getProgramName());
        p.setDescription(req.getDescription());
        return ProgramDto.from(programRepository.save(p));
    }

    @Transactional
    public void deactivate(String id) {
        Program p = programRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Programa", id));
        p.setIsActive(false);
        programRepository.save(p);
    }
}
