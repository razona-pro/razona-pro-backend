package com.razonapro.razonaprobackend.domain.ranking.dto.response;

import com.razonapro.razonaprobackend.domain.ranking.model.Ranking;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class RankingDto {
    private String rankingId;
    private String rankingName;
    private String description;
    private String periodType;
    private String sourceFilter;
    /** Competencias del ranking; vacío = todas (general). */
    private List<String> competenceIds;
    private Boolean isActive;

    public static RankingDto from(Ranking r) {
        return RankingDto.builder()
                .rankingId(r.getRankingId())
                .rankingName(r.getRankingName())
                .description(r.getDescription())
                .periodType(r.getPeriodType())
                .sourceFilter(r.getSourceFilter())
                .competenceIds(r.getCompetenceIds() == null ? List.of() : List.copyOf(r.getCompetenceIds()))
                .isActive(r.getIsActive())
                .build();
    }
}