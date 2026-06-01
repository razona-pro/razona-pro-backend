package com.razonapro.razonaprobackend.domain.student.service;

import com.razonapro.razonaprobackend.domain.student.dto.request.StudentUpdateRequest;
import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public PagedResponse<StudentDto> findAll(
            String search, String programId, String status, Pageable pageable) {
        String q  = (search    != null && !search.isBlank())    ? search.trim()    : null;
        String p  = (programId != null && !programId.isBlank()) ? programId.trim() : null;
        String sf = (status    != null && !status.isBlank())    ? status.trim()    : "";

        if (q == null && p == null && sf.isEmpty()) {
            return PagedResponse.from(studentRepository.findAll(pageable).map(StudentDto::from));
        }
        return PagedResponse.from(studentRepository.findByFilters(q, p, sf, pageable).map(StudentDto::from));
    }

    public PagedResponse<StudentDto> findAll(Pageable pageable) {
        return PagedResponse.from(studentRepository.findAll(pageable).map(StudentDto::from));
    }

    @Transactional
    public StudentDto activate(String studentId) {
        Student s = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));
        s.setIsActive(true);
        return StudentDto.from(studentRepository.save(s));
    }

    public StudentDto findById(String studentId) {
        return StudentDto.from(studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId)));
    }

    @Transactional
    public StudentDto update(String studentId, StudentUpdateRequest req) {
        Student s = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));

        if (StringUtils.hasText(req.getFirstName()))     s.setFirstName(req.getFirstName());
        if (StringUtils.hasText(req.getSecondName()))    s.setSecondName(req.getSecondName());
        if (StringUtils.hasText(req.getFirstSurname()))  s.setFirstSurname(req.getFirstSurname());
        if (StringUtils.hasText(req.getSecondSurname())) s.setSecondSurname(req.getSecondSurname());
        if (StringUtils.hasText(req.getPhone()))         s.setPhone(req.getPhone());
        if (req.getIsActive() != null)                   s.setIsActive(req.getIsActive());

        return StudentDto.from(studentRepository.save(s));
    }

    @Transactional
    public void deactivate(String studentId) {
        Student s = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));
        s.setIsActive(false);
        studentRepository.save(s);
    }
}