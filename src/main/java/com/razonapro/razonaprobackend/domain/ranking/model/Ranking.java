package com.razonapro.razonaprobackend.domain.ranking.model;

import com.razonapro.razonaprobackend.shared.jpa.Normalizable;
import com.razonapro.razonaprobackend.shared.jpa.NormalizingEntityListener;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "rankings")
@EntityListeners(NormalizingEntityListener.class)
public class Ranking implements Normalizable {

    @Id
    @Column(name = "ranking_id", length = 6)
    private String rankingId;

    @Column(name = "ranking_name", length = 20, nullable = false)
    private String rankingName;

    @Column(name = "description", length = 100)
    private String description;

    /** DAILY | WEEKLY | MONTHLY | GENERAL */
    @Column(name = "period_type", length = 10, nullable = false)
    private String periodType;

    /** ALL | TRIEDS | AI_TRIEDS */
    @Column(name = "source_filter", length = 10, nullable = false)
    private String sourceFilter;

    @Column(name = "is_active", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    @Override
    public void normalize() {
        rankingId   = StringNormalizer.upper(rankingId);
        rankingName = StringNormalizer.trim(rankingName);
        description = StringNormalizer.trim(description);
    }
}