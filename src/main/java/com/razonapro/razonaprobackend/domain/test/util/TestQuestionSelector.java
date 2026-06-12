package com.razonapro.razonaprobackend.domain.test.util;

import com.razonapro.razonaprobackend.domain.test.model.TestQuestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Selección de las preguntas que se PRESENTAN en un intento cuando la prueba define
 * "preguntas a presentar" menor que el banco.
 *
 * Es DETERMINISTA por triedId: el mismo intento siempre obtiene el mismo subconjunto.
 * Así las tres rutas coinciden EXACTAMENTE — iniciar el intento (total de preguntas),
 * presentarlas al estudiante (incluso tras recargar) y marcar las "sin responder" al
 * finalizar — y se evita que cada llamada baraje un subconjunto distinto.
 *
 * Si no hay límite (toPresent null o &gt;= total) devuelve TODAS, sin alterar el orden.
 */
public final class TestQuestionSelector {

    private TestQuestionSelector() {}

    public static List<TestQuestion> selectForTried(List<TestQuestion> all, Integer toPresent, String triedId) {
        if (all == null || all.isEmpty()) return all;
        if (toPresent == null || triedId == null || toPresent >= all.size()) {
            return all;
        }
        List<TestQuestion> ordered = new ArrayList<>(all);
        // Orden base ESTABLE (por PK) antes de barajar: garantiza el mismo subconjunto
        // sin importar el orden en que la BD devuelva las filas.
        ordered.sort(Comparator.comparing(TestQuestion::getTestQuestionId));
        Collections.shuffle(ordered, new Random((long) triedId.hashCode()));
        return new ArrayList<>(ordered.subList(0, toPresent));
    }
}
