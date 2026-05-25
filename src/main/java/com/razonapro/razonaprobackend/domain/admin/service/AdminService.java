package com.razonapro.razonaprobackend.domain.admin.service;

import com.razonapro.razonaprobackend.domain.admin.dto.request.AdminCreateRequest;
import com.razonapro.razonaprobackend.domain.admin.dto.request.AdminUpdateRequest;
import com.razonapro.razonaprobackend.domain.admin.dto.response.AdminDto;
import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ApiException;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public PagedResponse<AdminDto> findAll(Pageable pageable) {
        return PagedResponse.from(adminRepository.findAll(pageable).map(AdminDto::from));
    }

    public AdminDto findById(String id) {
        return AdminDto.from(adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id)));
    }

    @Transactional
    public AdminDto create(AdminCreateRequest req) {
        if (adminRepository.existsByEmail(req.getEmail().trim().toUpperCase()))
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        if (adminRepository.existsByPhone(req.getPhone().trim()))
            throw new ApiException(ErrorCode.PHONE_ALREADY_EXISTS);

        Admin admin = Admin.builder()
                .adminId(IdGenerator.adminId(adminRepository.count()))
                .firstName(req.getFirstName())
                .secondName(req.getSecondName())
                .firstSurname(req.getFirstSurname())
                .secondSurname(req.getSecondSurname())
                .email(req.getEmail())
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        return AdminDto.from(adminRepository.save(admin));
    }

    @Transactional
    public AdminDto update(String id, AdminUpdateRequest req) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id));

        if (StringUtils.hasText(req.getFirstName()))     admin.setFirstName(req.getFirstName());
        if (StringUtils.hasText(req.getSecondName()))    admin.setSecondName(req.getSecondName());
        if (StringUtils.hasText(req.getFirstSurname()))  admin.setFirstSurname(req.getFirstSurname());
        if (StringUtils.hasText(req.getSecondSurname())) admin.setSecondSurname(req.getSecondSurname());
        if (StringUtils.hasText(req.getPhone()))         admin.setPhone(req.getPhone());
        if (req.getIsActive() != null)                   admin.setIsActive(req.getIsActive());

        return AdminDto.from(adminRepository.save(admin));
    }

    @Transactional
    public void deactivate(String id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id));
        admin.setIsActive(false);
        adminRepository.save(admin);
    }
}