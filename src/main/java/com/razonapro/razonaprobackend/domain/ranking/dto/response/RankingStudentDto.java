package com.razonapro.razonaprobackend.domain.ranking.dto.response;

import com.razonapro.razonaprobackend.domain.ranking.model.RankingStudent;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @Builder
public class RankingStudentDto {
    private Integer rankingStudentId;
    private String studentId;
    private String studentName;
    private String programId;
    private BigDecimal totalScore;
    private BigDecimal triedsScore;
    private BigDecimal aiTriedsScore;
    private Integer triedsCount;
    private Integer aiTriedsCount;

    public static RankingStudentDto from(RankingStudent rs) {
        String name = rs.getStudent() != null
            ? rs.getStudent().getFirstName() + " " + rs.getStudent().getFirstSurname()
            : null;
        return RankingStudentDto.builder()
            .rankingStudentId(rs.getRankingStudentId())
            .studentId(rs.getStudentId())
            .studentName(name)
            .programId(rs.getProgramId())
            .totalScore(rs.getTotalScore())
            .triedsScore(rs.getTriedsScore())
            .aiTriedsScore(rs.getAiTriedsScore())
            .triedsCount(rs.getTriedsCount())
            .aiTriedsCount(rs.getAiTriedsCount())
            .build();
    }
}
