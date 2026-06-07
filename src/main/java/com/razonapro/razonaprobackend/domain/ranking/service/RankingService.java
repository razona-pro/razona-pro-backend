package com.razonapro.razonaprobackend.domain.ranking.service;

import com.razonapro.razonaprobackend.domain.ranking.dto.request.RankingRequest;
import com.razonapro.razonaprobackend.domain.ranking.dto.response.RankingDto;
import com.razonapro.razonaprobackend.domain.ranking.dto.response.RankingStudentDto;
import com.razonapro.razonaprobackend.domain.ranking.model.Ranking;
import com.razonapro.razonaprobackend.domain.ranking.model.RankingStudent;
import com.razonapro.razonaprobackend.domain.ranking.repository.RankingRepository;
import com.razonapro.razonaprobackend.domain.ranking.repository.RankingStudentRepository;
import com.razonapro.razonaprobackend.domain.tried.repository.TriedRepository;
import com.razonapro.razonaprobackend.domain.aitried.repository.AiTriedRepository;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRepository        rankingRepository;
    private final RankingStudentRepository rankingStudentRepository;
    private final TriedRepository          triedRepository;
    private final AiTriedRepository        aiTriedRepository;
    private final com.razonapro.razonaprobackend.domain.program.repository.ProgramRepository programRepository;

    /**
     * Recalcula los puntajes de un estudiante en todos los rankings activos.
     * Reemplaza al trigger PL/pgSQL fn_refresh_student_ranking (eliminado para no acoplar
     * la finalización de un intento a funciones de BD). Corre en transacción PROPIA
     * (REQUIRES_NEW) y los llamadores lo envuelven en try/catch: si algo falla aquí,
     * NUNCA debe impedir que un intento se finalice.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshForStudent(String studentId, String programId, LocalDateTime finishedAt) {
        LocalDateTime when = (finishedAt != null) ? finishedAt : LocalDateTime.now();

        List<Ranking> active = rankingRepository.findByIsActiveTrue();
        if (active.isEmpty()) {
            log.warn("refreshForStudent: no hay rankings ACTIVOS; nada que actualizar para {}/{}", programId, studentId);
            return;
        }

        for (Ranking r : active) {
            try {
                refreshOne(r, studentId, programId, when);
            } catch (Exception e) {
                // Un ranking con datos inconsistentes no debe impedir actualizar los demás.
                log.error("refreshForStudent: fallo actualizando ranking {} para {}/{}: {}",
                        r.getRankingId(), programId, studentId, e.getMessage(), e);
            }
        }
    }

    /** Actualiza (o crea) la fila del estudiante en UN ranking concreto. */
    private void refreshOne(Ranking r, String studentId, String programId, LocalDateTime when) {
        {
            LocalDate ps = null, pe = null;
            switch (r.getPeriodType() == null ? "GENERAL" : r.getPeriodType()) {
                case "DAILY"   -> { ps = when.toLocalDate(); pe = ps; }
                case "WEEKLY"  -> { ps = when.toLocalDate().with(DayOfWeek.MONDAY); pe = ps.plusDays(6); }
                case "MONTHLY" -> { ps = when.toLocalDate().withDayOfMonth(1); pe = ps.plusMonths(1).minusDays(1); }
                default        -> { ps = null; pe = null; }   // GENERAL
            }
            LocalDateTime start = (ps != null) ? ps.atStartOfDay()        : null;
            LocalDateTime end   = (pe != null) ? pe.atTime(23, 59, 59)    : null;
            String src = r.getSourceFilter() == null ? "ALL" : r.getSourceFilter();
            // Conjunto de competencias del ranking. Vacío = general (todas las competencias).
            java.util.Set<String> comps = r.getCompetenceIds();
            boolean general = (comps == null || comps.isEmpty());

            BigDecimal triedsScore = BigDecimal.ZERO; long triedsCount = 0;
            if (src.equals("ALL") || src.equals("TRIEDS")) {
                // General: suma el score del intento. Por competencias (una o varias):
                // suma los puntos ponderados de las respuestas correctas de ESAS competencias.
                Object[] row = general
                        ? first(triedRepository.sumTriedsForRanking(studentId, programId, start, end))
                        : first(triedRepository.sumTriedsByCompetencesForRanking(studentId, programId, comps, start, end));
                if (row != null) { triedsScore = toBig(row[0]); triedsCount = toLong(row[1]); }
            }
            BigDecimal aiScore = BigDecimal.ZERO; long aiCount = 0;
            if (src.equals("ALL") || src.equals("AI_TRIEDS")) {
                Object[] row = general
                        ? first(aiTriedRepository.sumAiForRanking(studentId, programId, null, start, end))
                        : first(aiTriedRepository.sumAiByCompetencesForRanking(studentId, programId, comps, start, end));
                if (row != null) { aiScore = toBig(row[0]); aiCount = toLong(row[1]); }
            }

            final LocalDate fps = ps, fpe = pe;
            RankingStudent rs = rankingStudentRepository
                    .findByRankingRankingIdAndStudentIdAndProgramId(r.getRankingId(), studentId, programId)
                    .stream()
                    .filter(x -> Objects.equals(x.getPeriodStart(), fps) && Objects.equals(x.getPeriodEnd(), fpe))
                    .findFirst()
                    .orElseGet(() -> RankingStudent.builder()
                            .ranking(r).studentId(studentId).programId(programId)
                            .periodStart(fps).periodEnd(fpe)
                            .build());

            rs.setTriedsScore(triedsScore);
            rs.setAiTriedsScore(aiScore);
            rs.setTriedsCount((int) triedsCount);
            rs.setAiTriedsCount((int) aiCount);
            rs.setTotalScore(triedsScore.add(aiScore));
            rs.setLastActivityAt(LocalDateTime.now());
            rankingStudentRepository.save(rs);
        }
    }

    private static Object[] first(List<Object[]> rows) { return (rows == null || rows.isEmpty()) ? null : rows.get(0); }
    private static BigDecimal toBig(Object o) { return (o instanceof Number n) ? new BigDecimal(n.toString()) : BigDecimal.ZERO; }
    private static long toLong(Object o) { return (o instanceof Number n) ? n.longValue() : 0L; }

    public List<RankingDto> findAll() {
        return rankingRepository.findAll().stream().map(RankingDto::from).toList();
    }

    public List<RankingDto> findAllActive() {
        return rankingRepository.findByIsActiveTrue().stream().map(RankingDto::from).toList();
    }

    public PagedResponse<RankingStudentDto> getLeaderboard(String rankingId, Pageable pageable) {
        rankingRepository.findById(rankingId)
                .orElseThrow(() -> new ResourceNotFoundException("Ranking", rankingId));
        // Mapa programId -> nombre (pocos programas) para mostrar el nombre, no el código.
        java.util.Map<String, String> programNames = new java.util.HashMap<>();
        programRepository.findAll().forEach(p -> programNames.put(p.getProgramId(), p.getProgramName()));
        return PagedResponse.from(
                rankingStudentRepository.findLeaderboard(rankingId, pageable)
                        .map(rs -> RankingStudentDto.from(rs, programNames.get(rs.getProgramId()))));
    }

    @Transactional
    public RankingDto create(RankingRequest req) {
        Ranking ranking = Ranking.builder()
                .rankingId(IdGenerator.rankingId(rankingRepository.count()))
                .rankingName(req.getRankingName())
                .description(req.getDescription())
                .periodType(req.getPeriodType())
                .sourceFilter(req.getSourceFilter())
                .competenceIds(cleanComps(req.getCompetenceIds()))
                .build();
        return RankingDto.from(rankingRepository.save(ranking));
    }

    @Transactional
    public void deactivate(String id) {
        Ranking r = rankingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ranking", id));
        r.setIsActive(false);
        rankingRepository.save(r);
    }

    /** Reactiva un ranking (como en las demás tablas con soft-delete). */
    @Transactional
    public RankingDto activate(String id) {
        Ranking r = rankingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ranking", id));
        r.setIsActive(true);
        return RankingDto.from(rankingRepository.save(r));
    }

    /** Edita los campos editables de un ranking. */
    @Transactional
    public RankingDto update(String id, RankingRequest req) {
        Ranking r = rankingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ranking", id));
        if (req.getRankingName()  != null) r.setRankingName(req.getRankingName());
        if (req.getDescription()  != null) r.setDescription(req.getDescription());
        if (req.getPeriodType()   != null) r.setPeriodType(req.getPeriodType());
        if (req.getSourceFilter() != null) r.setSourceFilter(req.getSourceFilter());
        if (req.getCompetenceIds() != null) {
            r.getCompetenceIds().clear();
            r.getCompetenceIds().addAll(cleanComps(req.getCompetenceIds()));
        }
        return RankingDto.from(rankingRepository.save(r));
    }

    /** Normaliza la lista de competencias: trim + UPPER, sin vacíos ni duplicados; conserva el orden. */
    private static java.util.Set<String> cleanComps(java.util.List<String> raw) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        if (raw != null) {
            for (String c : raw) {
                if (c != null && !c.isBlank()) out.add(c.trim().toUpperCase());
            }
        }
        return out;
    }
}