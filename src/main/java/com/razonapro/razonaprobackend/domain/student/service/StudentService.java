// domain/student/service/StudentService.java
package com.razonapro.razonaprobackend.domain.student.service;

import com.razonapro.razonaprobackend.domain.student.dto.response.StudentDto;
import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import com.razonapro.razonaprobackend.domain.student.dto.request.StudentUpdateRequest;
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

    public PagedResponse<StudentDto> findAll(Pageable pageable) {
        return PagedResponse.from(studentRepository.findAll(pageable).map(StudentDto::from));
    }

    public StudentDto findById(String studentId) {
        return StudentDto.from(studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId)));
    }

    @Transactional
    public StudentDto update(String studentId, StudentUpdateRequest req) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));
        if (StringUtils.hasText(req.getFirstName()))    student.setFirstName(req.getFirstName().trim().toUpperCase());
        if (StringUtils.hasText(req.getSecondName()))   student.setSecondName(req.getSecondName().trim().toUpperCase());
        if (StringUtils.hasText(req.getFirstSurname())) student.setFirstSurname(req.getFirstSurname().trim().toUpperCase());
        if (StringUtils.hasText(req.getSecondSurname()))student.setSecondSurname(req.getSecondSurname().trim().toUpperCase());
        if (StringUtils.hasText(req.getPhone()))        student.setPhone(req.getPhone().trim());
        if (req.getIsActive() != null)                  student.setIsActive(req.getIsActive());
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