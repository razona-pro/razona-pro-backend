package com.razonapro.razonaprobackend.dtos.response;

import com.razonapro.razonaprobackend.models.Ranking;
import com.razonapro.razonaprobackend.models.RankingStudent;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @Builder
public class RankingDto {
    private String rankingId;
    private String rankingName;
    private String description;
    private String periodType;
    private String sourceFilter;
    private Boolean isActive;

    public static RankingDto from(Ranking r) {
        return RankingDto.builder()
            .rankingId(r.getRankingId())
            .rankingName(r.getRankingName())
            .description(r.getDescription())
            .periodType(r.getPeriodType())
            .sourceFilter(r.getSourceFilter())
            .isActive(r.getIsActive())
            .build();
    }
}
