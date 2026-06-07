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
        // Cadenas vacías (no null) para evitar parámetros de tipo indefinido en PostgreSQL
        // (un null sin tipo se infiere como bytea y rompe LOWER(...)).
        String q  = (search    != null) ? search.trim()    : "";
        String p  = (programId != null) ? programId.trim() : "";
        String sf = (status    != null) ? status.trim()    : "";

        if (q.isEmpty() && p.isEmpty() && sf.isEmpty()) {
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
        s.setDeactivationReason(null);   // al reactivar se limpia el motivo
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
        if (StringUtils.hasText(req.getEmail())) {
            String email = req.getEmail().trim().toLowerCase();
            if (!email.equalsIgnoreCase(s.getEmail()) && studentRepository.existsByEmailIgnoreCase(email))
                throw new com.razonapro.razonaprobackend.shared.exception.ApiException(
                        com.razonapro.razonaprobackend.shared.exception.ErrorCode.EMAIL_ALREADY_EXISTS);
            s.setEmail(email);
        }
        if (StringUtils.hasText(req.getPhone()))         s.setPhone(req.getPhone().trim());
        if (req.getIsActive() != null) {
            s.setIsActive(req.getIsActive());
            // Reactivar limpia el motivo; desactivar manualmente lo marca como MANUAL.
            s.setDeactivationReason(Boolean.TRUE.equals(req.getIsActive()) ? null : "MANUAL");
        }

        return StudentDto.from(studentRepository.save(s));
    }

    /** Auto-edición del estudiante: solo nombres y celular (NO email, NO estado). */
    @Transactional
    public StudentDto updateOwnProfile(String studentId, StudentUpdateRequest req) {
        Student s = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));

        if (StringUtils.hasText(req.getFirstName()))     s.setFirstName(req.getFirstName().trim());
        if (req.getSecondName() != null)                 s.setSecondName(req.getSecondName().isBlank() ? null : req.getSecondName().trim());
        if (StringUtils.hasText(req.getFirstSurname()))  s.setFirstSurname(req.getFirstSurname().trim());
        if (req.getSecondSurname() != null)              s.setSecondSurname(req.getSecondSurname().isBlank() ? null : req.getSecondSurname().trim());
        if (StringUtils.hasText(req.getPhone())) {
            String phone = req.getPhone().trim();
            if (!phone.equals(s.getPhone()) && studentRepository.existsByPhone(phone))
                throw new com.razonapro.razonaprobackend.shared.exception.ApiException(
                        com.razonapro.razonaprobackend.shared.exception.ErrorCode.PHONE_ALREADY_EXISTS);
            s.setPhone(phone);
        }
        return StudentDto.from(studentRepository.save(s));
    }

    @Transactional
    public void deactivate(String studentId) {
        Student s = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));
        s.setIsActive(false);
        s.setDeactivationReason("MANUAL");
        studentRepository.save(s);
    }
}