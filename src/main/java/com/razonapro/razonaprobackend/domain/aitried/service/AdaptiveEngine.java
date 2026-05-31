// src/main/java/com/razonapro/razonaprobackend/domain/aitried/service/AdaptiveEngine.java
package com.razonapro.razonaprobackend.domain.aitried.service;

import org.springframework.stereotype.Component;

/**
 * Motor adaptativo basado en una simplificación del modelo Rasch / ELO educativo.
 *
 * θ (theta) representa la habilidad del estudiante [-3, +3].
 * Dificultad 1-10 se mapea linealmente a θ [-2, +2].
 * Actualización: θ_nuevo = θ + K * (resultado - P)
 *   donde P = sigmoid(θ - b) es la probabilidad esperada de acierto.
 */
@Component
public class AdaptiveEngine {

    private static final double THETA_MIN = -3.0;
    private static final double THETA_MAX =  3.0;
    private static final double K_FACTOR  =  0.4;   // tasa de aprendizaje

    /** Calcula la siguiente dificultad (1-10) a partir del θ actual. */
    public int nextDifficulty(double theta) {
        // Mapea θ [-3, +3] → dificultad [1, 10]
        double norm = (theta - THETA_MIN) / (THETA_MAX - THETA_MIN); // 0..1
        return (int) Math.round(1 + norm * 9);
    }

    /**
     * Actualiza θ según si la respuesta fue correcta o no.
     *
     * @param theta          habilidad actual del estudiante
     * @param difficultyLevel dificultad de la pregunta respondida (1-10)
     * @param correct         si respondió correctamente
     * @return nuevo θ
     */
    public double updateTheta(double theta, int difficultyLevel, boolean correct) {
        double b = difficultyToTheta(difficultyLevel);   // dificultad en escala θ
        double p = sigmoid(theta - b);                    // prob. esperada de acierto
        int    result = correct ? 1 : 0;
        double newTheta = theta + K_FACTOR * (result - p);
        return clamp(newTheta, THETA_MIN, THETA_MAX);
    }

    /** Mapea dificultad 1-10 a escala θ [-2, +2]. */
    private double difficultyToTheta(int level) {
        return -2.0 + ((level - 1) / 9.0) * 4.0;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}