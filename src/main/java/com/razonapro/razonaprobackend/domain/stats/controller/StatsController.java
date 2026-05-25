package com.razonapro.razonaprobackend.domain.stats.controller;

import com.razonapro.razonaprobackend.domain.stats.dto.HomeStatsDto;
import com.razonapro.razonaprobackend.domain.stats.service.StatsService;
import com.razonapro.razonaprobackend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Estadísticas públicas")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/home")
    @Operation(summary = "Métricas agregadas para el home (público)")
    public ResponseEntity<ApiResponse<HomeStatsDto>> home() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.homeStats()));
    }
}