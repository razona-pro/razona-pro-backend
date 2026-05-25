package com.razonapro.razonaprobackend.domain.ranking.controller;

import com.razonapro.razonaprobackend.domain.ranking.dto.request.RankingRequest;
import com.razonapro.razonaprobackend.domain.ranking.dto.response.RankingDto;
import com.razonapro.razonaprobackend.domain.ranking.dto.response.RankingStudentDto;
import com.razonapro.razonaprobackend.domain.ranking.service.RankingService;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import com.razonapro.razonaprobackend.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@Tag(name = "Rankings", description = "Rankings y leaderboards")
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<List<RankingDto>>> findAll(
            @RequestParam(required = false) Boolean active) {
        List<RankingDto> data = Boolean.TRUE.equals(active)
                ? rankingService.findAllActive()
                : rankingService.findAll();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{rankingId}/leaderboard")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<RankingStudentDto>>> getLeaderboard(
            @PathVariable String rankingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                rankingService.getLeaderboard(rankingId, PageRequest.of(page, size))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RankingDto>> create(@Valid @RequestBody RankingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(rankingService.create(req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        rankingService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Ranking desactivado"));
    }
}