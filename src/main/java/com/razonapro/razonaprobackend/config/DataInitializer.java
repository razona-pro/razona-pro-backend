package com.razonapro.razonaprobackend.config;

import com.razonapro.razonaprobackend.models.*;
import com.razonapro.razonaprobackend.repositories.*;
import com.razonapro.razonaprobackend.util.IdGenerator;
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

    private final AdminRepository      adminRepository;
    private final ProgramRepository    programRepository;
    private final CompetenceRepository competenceRepository;
    private final RankingRepository    rankingRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AppProperties        appProperties;

    @Override
    @Transactional
    public void run(String... args) {
        if (!appProperties.getAdminInitializer().isEnabled()) {
            log.info("DataInitializer deshabilitado.");
            return;
        }
        initPrograms();
        initCompetences();
        initRankings();
        initSuperAdmin();
    }

    // ── Programas ─────────────────────────────────────────────
    private void initPrograms() {
        if (programRepository.count() > 0) return;
        log.info("Inicializando programas...");

        programRepository.save(Program.builder()
            .programId("SIS")
            .programName("Ingeniería de Sistemas")
            .description("Tecnología, software y desarrollo")
            .build());

        programRepository.save(Program.builder()
            .programId("ELE")
            .programName("Ingeniería Eléctrica")
            .description("Sistemas eléctricos y electrónica")
            .build());

        programRepository.save(Program.builder()
            .programId("IND")
            .programName("Ingeniería Industrial")
            .description("Procesos industriales y gestión")
            .build());

        programRepository.save(Program.builder()
            .programId("ADM")
            .programName("Administración")
            .description("Gestión y negocios")
            .build());

        log.info("4 programas creados.");
    }

    // ── Competencias ──────────────────────────────────────────
    private void initCompetences() {
        if (competenceRepository.count() > 0) return;
        log.info("Inicializando competencias...");

        competenceRepository.save(Competence.builder()
            .competenceId("COM001")
            .competenceName("Razonamiento Lógico")
            .description("Análisis y resolución lógica de problemas")
            .build());

        competenceRepository.save(Competence.builder()
            .competenceId("COM002")
            .competenceName("Comunicación Escrita")
            .description("Comprensión lectora y producción textual")
            .build());

        competenceRepository.save(Competence.builder()
            .competenceId("COM003")
            .competenceName("Razonamiento Cuantitativo")
            .description("Matemáticas y estadística aplicada")
            .build());

        competenceRepository.save(Competence.builder()
            .competenceId("COM004")
            .competenceName("Inglés")
            .description("Comprensión de textos en inglés")
            .build());

        log.info("4 competencias creadas.");
    }

    // ── Rankings ──────────────────────────────────────────────
    private void initRankings() {
        if (rankingRepository.count() > 0) return;
        log.info("Inicializando rankings...");

        rankingRepository.save(Ranking.builder()
            .rankingId("RNK001")
            .rankingName("Semanal Global")
            .description("Top estudiantes de la semana (tests + AI)")
            .periodType("WEEKLY")
            .sourceFilter("ALL")
            .build());

        rankingRepository.save(Ranking.builder()
            .rankingId("RNK002")
            .rankingName("Mensual Global")
            .description("Top estudiantes del mes (tests + AI)")
            .periodType("MONTHLY")
            .sourceFilter("ALL")
            .build());

        rankingRepository.save(Ranking.builder()
            .rankingId("RNK003")
            .rankingName("General Histórico")
            .description("Acumulado histórico de todos los intentos")
            .periodType("GENERAL")
            .sourceFilter("ALL")
            .build());

        rankingRepository.save(Ranking.builder()
            .rankingId("RNK004")
            .rankingName("Semanal Tests")
            .description("Top semanal solo de tests formales")
            .periodType("WEEKLY")
            .sourceFilter("TRIEDS")
            .build());

        log.info("4 rankings creados.");
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
