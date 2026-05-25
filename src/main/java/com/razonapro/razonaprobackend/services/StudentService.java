package com.razonapro.razonaprobackend.services;

import com.razonapro.razonaprobackend.dtos.request.StudentUpdateRequest;
import com.razonapro.razonaprobackend.dtos.response.PagedResponse;
import com.razonapro.razonaprobackend.dtos.response.StudentDto;
import com.razonapro.razonaprobackend.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.models.Student;
import com.razonapro.razonaprobackend.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public PagedResponse<StudentDto> findAll(Pageable pageable) {
        Page<StudentDto> page = studentRepository.findAll(pageable).map(StudentDto::from);
        return PagedResponse.from(page);
    }

    public StudentDto findById(String studentId) {
        return StudentDto.from(
                studentRepository.findByStudentId(studentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId))
        );
    }

    @Transactional
    public StudentDto update(String studentId, StudentUpdateRequest req) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));

        if (StringUtils.hasText(req.getFirstName()))
            student.setFirstName(req.getFirstName().trim().toUpperCase());
        if (StringUtils.hasText(req.getSecondName()))
            student.setSecondName(req.getSecondName().trim().toUpperCase());
        if (StringUtils.hasText(req.getFirstSurname()))
            student.setFirstSurname(req.getFirstSurname().trim().toUpperCase());
        if (StringUtils.hasText(req.getSecondSurname()))
            student.setSecondSurname(req.getSecondSurname().trim().toUpperCase());
        if (StringUtils.hasText(req.getPhone()))
            student.setPhone(req.getPhone().trim());
        if (req.getIsActive() != null)
            student.setIsActive(req.getIsActive());

        return StudentDto.from(studentRepository.save(student));
    }

    @Transactional
    public void deactivate(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));
        student.setIsActive(false);
        studentRepository.save(student);
    }
}