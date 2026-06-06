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

        for (Ranking r : rankingRepository.findByIsActiveTrue()) {
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
            // Ranking por competencia: si competenceId != null, solo cuenta esa competencia.
            String comp = (r.getCompetenceId() == null || r.getCompetenceId().isBlank())
                    ? null : r.getCompetenceId();

            BigDecimal triedsScore = BigDecimal.ZERO; long triedsCount = 0;
            if (src.equals("ALL") || src.equals("TRIEDS")) {
                // General: suma el score del intento. Por competencia (multicompetencia):
                // suma los puntos ponderados de las respuestas correctas de esa competencia.
                Object[] row = (comp == null)
                        ? first(triedRepository.sumTriedsForRanking(studentId, programId, start, end))
                        : first(triedRepository.sumTriedsByCompetenceForRanking(studentId, programId, comp, start, end));
                if (row != null) { triedsScore = toBig(row[0]); triedsCount = toLong(row[1]); }
            }
            BigDecimal aiScore = BigDecimal.ZERO; long aiCount = 0;
            if (src.equals("ALL") || src.equals("AI_TRIEDS")) {
                Object[] row = first(aiTriedRepository.sumAiForRanking(studentId, programId, comp, start, end));
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
        return PagedResponse.from(
                rankingStudentRepository.findLeaderboard(rankingId, pageable).map(RankingStudentDto::from));
    }

    @Transactional
    public RankingDto create(RankingRequest req) {
        Ranking ranking = Ranking.builder()
                .rankingId(IdGenerator.rankingId(rankingRepository.count()))
                .rankingName(req.getRankingName())
                .description(req.getDescription())
                .periodType(req.getPeriodType())
                .sourceFilter(req.getSourceFilter())
                .competenceId((req.getCompetenceId() == null || req.getCompetenceId().isBlank())
                        ? null : req.getCompetenceId().trim())
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
        r.setCompetenceId((req.getCompetenceId() == null || req.getCompetenceId().isBlank())
                ? null : req.getCompetenceId().trim());
        return RankingDto.from(rankingRepository.save(r));
    }
}