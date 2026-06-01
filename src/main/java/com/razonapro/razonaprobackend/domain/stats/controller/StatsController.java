package com.razonapro.razonaprobackend.domain.stats.controller;

import com.razonapro.razonaprobackend.domain.stats.dto.*;
import com.razonapro.razonaprobackend.domain.stats.service.StatsService;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Estadísticas públicas y de administración")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/home")
    @Operation(summary = "Métricas agregadas para el home (público)")
    public ResponseEntity<ApiResponse<HomeStatsDto>> home() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.homeStats()));
    }

    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumen general para el panel de administración")
    public ResponseEntity<ApiResponse<AdminOverviewDto>> adminOverview() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.adminOverview()));
    }

    @GetMapping("/admin/student-performance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rendimiento por estudiante (intentos finalizados)")
    public ResponseEntity<ApiResponse<List<StudentPerformanceDto>>> studentPerformance() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.studentPerformance()));
    }

    @GetMapping("/admin/question-trends")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tendencias de respuesta por pregunta")
    public ResponseEntity<ApiResponse<List<QuestionTrendDto>>> questionTrends(
            @RequestParam(required = false) String competenceId) {
        return ResponseEntity.ok(ApiResponse.ok(statsService.questionTrends(competenceId)));
    }
}
