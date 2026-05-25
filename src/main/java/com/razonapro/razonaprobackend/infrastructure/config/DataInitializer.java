package com.razonapro.razonaprobackend.infrastructure.config;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import com.razonapro.razonaprobackend.domain.competence.repository.CompetenceRepository;
import com.razonapro.razonaprobackend.domain.program.model.Program;
import com.razonapro.razonaprobackend.domain.program.repository.ProgramRepository;
import com.razonapro.razonaprobackend.domain.ranking.model.Ranking;
import com.razonapro.razonaprobackend.domain.ranking.repository.RankingRepository;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
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
    private final ProgramRepository programRepository;
    private final CompetenceRepository competenceRepository;
    private final RankingRepository rankingRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AppProperties        appProperties;

    @Override
    @Transactional
    public void run(String... args) {
        if (!appProperties.getAdminInitializer().isEnabled()) {
            log.info("DataInitializer deshabilitado.");
            return;
        }
        initSuperAdmin();
    }

    // ── Super Admin ───────────────────────────────────────────
    private void initSuperAdmin() {
        AppProperties.AdminInitializer cfg = appProperties.getAdminInitializer();
        if (adminRepository.existsByEmail(cfg.getEmail())) {
            log.info("Super admin ya existe: {}", cfg.getEmail());
            return;
        }
        log.info("Creando super admin: {}", cfg.getEmail());

        Admin admin = Admin.builder()
            .adminId(IdGenerator.adminId(0))
            .firstName(cfg.getFirstName())
            .firstSurname(cfg.getLastName())
            .email(cfg.getEmail())
            .phone(appProperties.getAdminInitializer().getPhone() != null
                ? appProperties.getAdminInitializer().getPhone()
                : "+573000000000")
            .passwordHash(passwordEncoder.encode(cfg.getPassword()))
            .build();

        adminRepository.save(admin);
        log.info("✅ Super admin creado — email: {} | password: [from env]", cfg.getEmail());
    }
}
