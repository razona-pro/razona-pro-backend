package com.razonapro.razonaprobackend.infrastructure.config;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties   appProperties;

    @Override
    @Transactional
    public void run(String... args) {
        if (!appProperties.getAdminInitializer().isEnabled()) {
            log.info("DataInitializer deshabilitado.");
            return;
        }
        initSuperAdmin();
    }

    private void initSuperAdmin() {
        AppProperties.AdminInitializer cfg = appProperties.getAdminInitializer();
        String email = StringNormalizer.upper(cfg.getEmail());
        if (adminRepository.existsByEmail(email)) {
            log.info("Super admin ya existe: {}", email);
            return;
        }
        log.info("Creando super admin: {}", email);
        Admin admin = Admin.builder()
                .adminId(IdGenerator.adminId(adminRepository.count()))
                .firstName(StringNormalizer.upper(cfg.getFirstName()))
                .firstSurname(StringNormalizer.upper(cfg.getLastName()))
                .email(email)
                .phone(StringNormalizer.trim(cfg.getPhone() != null ? cfg.getPhone() : "+573000000000"))
                .passwordHash(passwordEncoder.encode(cfg.getPassword()))
                .build();
        adminRepository.save(admin);
        log.info("Super admin creado — adminId: {}", admin.getAdminId());
    }
}