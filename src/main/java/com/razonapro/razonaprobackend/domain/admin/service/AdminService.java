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
    private final com.razonapro.razonaprobackend.infrastructure.email.EmailService emailService;

    public PagedResponse<AdminDto> findAll(String search, String status, Pageable pageable) {
        String q  = (search != null && !search.isBlank()) ? search.trim() : null;
        String sf = (status != null && !status.isBlank()) ? status.trim() : "";

        if (q == null && sf.isEmpty()) {
            return PagedResponse.from(adminRepository.findAll(pageable).map(AdminDto::from));
        }
        return PagedResponse.from(adminRepository.findByFilters(q, sf, pageable).map(AdminDto::from));
    }

    public PagedResponse<AdminDto> findAll(Pageable pageable) {
        return PagedResponse.from(adminRepository.findAll(pageable).map(AdminDto::from));
    }

    public AdminDto findById(String id) {
        return AdminDto.from(adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id)));
    }

    @Transactional
    public AdminDto create(AdminCreateRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (adminRepository.existsByEmail(email))
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);

        // La contraseña se genera y se envía por correo (no la digita el admin creador).
        String rawPassword = generatePassword();

        Admin admin = Admin.builder()
                .adminId(IdGenerator.adminId(adminRepository.count()))
                .firstName(req.getFirstName())
                .secondName(req.getSecondName())
                .firstSurname(req.getFirstSurname())
                .secondSurname(req.getSecondSurname())
                .email(email)
                .phone(null)   // el teléfono ya no se solicita
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
        Admin saved = adminRepository.save(admin);

        // Enviar credenciales por correo (best-effort: no rompe la creación si el correo falla).
        try {
            String name = (saved.getFirstName() + " " + saved.getFirstSurname()).trim();
            emailService.sendAdminCredentialsEmail(saved.getEmail(), name, rawPassword);
        } catch (Exception ignored) { /* el correo es @Async; cualquier fallo no debe romper la creación */ }

        return AdminDto.from(saved);
    }

    /** Genera una contraseña segura con al menos 1 minúscula, 1 mayúscula y 1 dígito. */
    private String generatePassword() {
        final String lower = "abcdefghijkmnpqrstuvwxyz";
        final String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String digit = "23456789";
        final String all   = lower + upper + digit;
        var rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(digit.charAt(rnd.nextInt(digit.length())));
        for (int i = 0; i < 9; i++) sb.append(all.charAt(rnd.nextInt(all.length())));
        // Mezclar para que el orden no sea predecible
        java.util.List<Character> chars = new java.util.ArrayList<>();
        for (char c : sb.toString().toCharArray()) chars.add(c);
        java.util.Collections.shuffle(chars, rnd);
        StringBuilder out = new StringBuilder();
        chars.forEach(out::append);
        return out.toString();
    }

    @Transactional
    public AdminDto update(String id, AdminUpdateRequest req) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id));

        if (StringUtils.hasText(req.getFirstName()))     admin.setFirstName(req.getFirstName());
        if (StringUtils.hasText(req.getSecondName()))    admin.setSecondName(req.getSecondName());
        if (StringUtils.hasText(req.getFirstSurname()))  admin.setFirstSurname(req.getFirstSurname());
        if (StringUtils.hasText(req.getSecondSurname())) admin.setSecondSurname(req.getSecondSurname());
        if (StringUtils.hasText(req.getEmail())) {
            String email = req.getEmail().trim().toLowerCase();
            if (!email.equals(admin.getEmail()) && adminRepository.existsByEmail(email))
                throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
            admin.setEmail(email);
        }
        if (StringUtils.hasText(req.getPhone()))         admin.setPhone(req.getPhone().trim());
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