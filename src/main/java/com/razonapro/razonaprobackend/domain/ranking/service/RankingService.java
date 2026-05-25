package com.razonapro.razonaprobackend.domain.ranking.service;

import com.razonapro.razonaprobackend.domain.ranking.dto.request.RankingRequest;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import com.razonapro.razonaprobackend.domain.ranking.dto.response.RankingDto;
import com.razonapro.razonaprobackend.domain.ranking.dto.response.RankingStudentDto;
import com.razonapro.razonaprobackend.shared.exception.ResourceNotFoundException;
import com.razonapro.razonaprobackend.domain.ranking.model.Ranking;
import com.razonapro.razonaprobackend.domain.ranking.repository.RankingRepository;
import com.razonapro.razonaprobackend.domain.ranking.repository.RankingStudentRepository;
import com.razonapro.razonaprobackend.infrastructure.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRepository        rankingRepository;
    private final RankingStudentRepository rankingStudentRepository;

    public List<RankingDto> findAll() {
        return rankingRepository.findAll().stream().map(RankingDto::from).toList();
    }

    public List<RankingDto> findAllActive() {
        return rankingRepository.findByIsActiveTrue().stream().map(RankingDto::from).toList();
    }

    public PagedResponse<RankingStudentDto> getLeaderboard(String rankingId, Pageable pageable) {
        rankingRepository.findById(rankingId)
            .orElseThrow(() -> new ResourceNotFoundException("Ranking", rankingId));
        Page<RankingStudentDto> page = rankingStudentRepository
            .findLeaderboard(rankingId, pageable)
            .map(RankingStudentDto::from);
        return PagedResponse.from(page);
    }

    @Transactional
    public RankingDto create(RankingRequest req) {
        long count = rankingRepository.count();
        Ranking ranking = Ranking.builder()
                .rankingId(IdGenerator.rankingId(count))
                .rankingName(req.getRankingName().trim().toUpperCase())
                .description(req.getDescription() != null ? req.getDescription().trim().toUpperCase() : null)
                .periodType(req.getPeriodType())
                .sourceFilter(req.getSourceFilter())
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
}
