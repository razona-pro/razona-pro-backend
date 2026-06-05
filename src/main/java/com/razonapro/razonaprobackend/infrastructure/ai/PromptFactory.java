// src/main/java/com/razonapro/razonaprobackend/infrastructure/ai/PromptFactory.java
package com.razonapro.razonaprobackend.infrastructure.ai;

import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptFactory {

    public static final String BATCH_SYSTEM = """
        Eres un experto en evaluación educativa del ICFES Colombia, especializado en el examen Saber Pro.
        Generas preguntas de selección múltiple (UNA sola respuesta correcta), de alta calidad,
        alineadas a la competencia y nivel de dificultad solicitados.

        REGLAS:
        1. Responde ÚNICAMENTE con JSON válido. Sin markdown, sin texto fuera del JSON.
        2. Cada pregunta es autosuficiente.
        3. Las 4 opciones son plausibles; exactamente UNA correcta.
        4. "explanation" justifica la correcta y por qué las otras no.
        5. Español neutro, académico. El enunciado tiene al menos 30 palabras.
        6. NO repitas ni parafrasees los enunciados a evitar.
        7. NO uses el guion largo (—) ni rayas: usa siempre un guion normal (-). Prohibido usar emojis.

        ESQUEMA JSON EXACTO (un objeto con un array "questions"):
        {
          "questions": [
            {
              "statement": "Enunciado completo...",
              "options": [
                {"text": "Opción A", "correct": false},
                {"text": "Opción B", "correct": true},
                {"text": "Opción C", "correct": false},
                {"text": "Opción D", "correct": false}
              ],
              "explanation": "Por qué B es correcta y las demás no...",
              "difficulty": 5
            }
          ]
        }
        """;

    public static final String HINT_SYSTEM = """
        Eres un tutor académico del Saber Pro. Das pistas que ayudan a razonar SIN revelar la respuesta.
        NIVELES:
        - Nivel 1: Orientación conceptual. No insinúes la opción correcta.
        - Nivel 2: Descompón el problema o señala la info clave del enunciado.
        - Nivel 3: Pista fuerte; razonamiento hacia la respuesta sin decir la letra. Puedes descartar opciones obvias.
        Responde SOLO el texto de la pista, sin "Pista:", sin comillas. Máximo 4 oraciones.
        NO uses el guion largo (—) ni rayas: usa siempre un guion normal (-). Prohibido usar emojis.
        """;

    /** Prompt para generar el batch completo de N preguntas en una sola llamada. */
    public String buildBatchPrompt(String competenceName, String competenceDescription,
                                   int totalQuestions, int baseDifficulty) {
        String compDesc = (competenceDescription != null && !competenceDescription.isBlank())
                ? "\nDescripción: " + competenceDescription : "";
        return """
            Genera EXACTAMENTE %d preguntas Saber Pro para la competencia:

            Competencia: %s%s
            Dificultad base sugerida: %d/10 (varía ±2 entre preguntas para progresión natural).

            Devuelve el objeto JSON con el array "questions" de %d elementos, según el esquema indicado.
            Cada pregunta con 4 opciones (una correcta) y su "difficulty" entre 1 y 10.
            Asegúrate de que las preguntas NO se repitan entre sí.
            """.formatted(totalQuestions, competenceName, compDesc, baseDifficulty, totalQuestions);
    }

    public String buildHintUserPrompt(AiHintContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Competencia: ").append(ctx.competenceName()).append("\n\n");
        sb.append("ENUNCIADO:\n").append(ctx.questionStatement()).append("\n\nOPCIONES:\n");
        List<String> opts = ctx.optionTexts();
        for (int i = 0; i < opts.size(); i++)
            sb.append((char) ('A' + i)).append(") ").append(opts.get(i)).append("\n");
        sb.append("\nNivel de pista: ").append(ctx.hintLevel()).append("\n");
        sb.append("""

            [Contexto interno — NO revelar]: La respuesta correcta es: %s
            Genera la pista del nivel %d.
            """.formatted(ctx.correctOptionText(), ctx.hintLevel()));
        return sb.toString();
    }
}