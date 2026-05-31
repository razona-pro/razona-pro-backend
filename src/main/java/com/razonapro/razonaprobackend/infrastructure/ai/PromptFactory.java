// src/main/java/com/razonapro/razonaprobackend/infrastructure/ai/PromptFactory.java
package com.razonapro.razonaprobackend.infrastructure.ai;

import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiHintContext;
import com.razonapro.razonaprobackend.domain.aitried.port.dto.AiQuestionRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptFactory {

    // ── SISTEMA base para generación de preguntas ──────────────────────

    public static final String QUESTION_SYSTEM = """
        Eres un experto en evaluación educativa del ICFES Colombia, especializado en el examen Saber Pro.
        Tu tarea es generar preguntas de selección múltiple (una sola respuesta correcta) de alta calidad,
        alineadas con las competencias genéricas del Saber Pro y el nivel de dificultad solicitado.
        
        REGLAS OBLIGATORIAS:
        1. Responde ÚNICAMENTE con JSON válido, sin texto adicional, sin markdown, sin explicaciones fuera del JSON.
        2. La pregunta debe ser autosuficiente (no requiere material externo a menos que lo incluyas en el enunciado).
        3. Las cuatro opciones deben ser plausibles y no "obviamente" incorrectas.
        4. La opción correcta debe estar bien respaldada por la teoría o el razonamiento explícito.
        5. El campo "explanation" explica por qué la respuesta correcta es la mejor y por qué las demás son incorrectas.
        6. Redacta en español neutro, académico, sin regionalismos.
        7. NO generes preguntas de trivia, opinión o de cultura general sin respaldo académico.
        8. El enunciado debe tener al menos 30 palabras.
        
        ESQUEMA JSON REQUERIDO (devuelve exactamente esto, sin campos extra):
        {
          "statement": "Texto completo del enunciado de la pregunta...",
          "options": [
            {"text": "Opción A", "correct": false},
            {"text": "Opción B", "correct": true},
            {"text": "Opción C", "correct": false},
            {"text": "Opción D", "correct": false}
          ],
          "explanation": "Explicación pedagógica de por qué B es correcta y las demás no..."
        }
        """;

    // ── SISTEMA base para pistas ────────────────────────────────────────

    public static final String HINT_SYSTEM = """
        Eres un tutor académico experto en el Saber Pro. Tu tarea es proporcionar pistas
        que ayuden al estudiante a razonar la respuesta SIN revelarla directamente.
        
        NIVELES DE PISTA:
        - Nivel 1: Orientación conceptual. Indica qué concepto o habilidad evalúa la pregunta.
          NO des ninguna pista sobre cuál opción es correcta.
        - Nivel 2: Descomposición. Descompón el problema en pasos o señala qué información
          del enunciado es clave. Aún NO reveles la respuesta.
        - Nivel 3: Pista fuerte. Señala el razonamiento que lleva a la respuesta sin
          decir explícitamente cuál letra es correcta. Puedes eliminar opciones obviamente incorrectas.
        
        Responde SOLO con el texto de la pista, sin introducción, sin "Pista:", sin comillas.
        Sé conciso (máximo 4 oraciones) y didáctico.
        """;

    // ── Builders ────────────────────────────────────────────────────────

    public String buildQuestionUserPrompt(AiQuestionRequest req) {
        String difficultyDesc = mapDifficulty(req.difficultyLevel());
        String avoidSection = req.statementsToAvoid() != null && !req.statementsToAvoid().isEmpty()
                ? "\n\nTEMAS/ENUNCIADOS A EVITAR (no repitas ni parafrasees estos):\n"
                + String.join("\n- ", req.statementsToAvoid().stream()
                .map(s -> s.length() > 80 ? s.substring(0, 80) + "..." : s)
                .toList())
                : "";

        String compDesc = (req.competenceDescription() != null && !req.competenceDescription().isBlank())
                ? "\nDescripción de la competencia: " + req.competenceDescription()
                : "";

        return """
            Genera una pregunta Saber Pro para la siguiente competencia:
            
            Competencia: %s%s
            Nivel de dificultad solicitado: %d/10 — %s%s
            
            Genera exactamente 4 opciones (una correcta, tres incorrectas pero plausibles).
            Devuelve el JSON con el esquema indicado.
            """.formatted(
                req.competenceName(),
                compDesc,
                req.difficultyLevel(),
                difficultyDesc,
                avoidSection
        );
    }

    public String buildHintUserPrompt(AiHintContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Competencia evaluada: ").append(ctx.competenceName()).append("\n\n");
        sb.append("ENUNCIADO DE LA PREGUNTA:\n").append(ctx.questionStatement()).append("\n\n");
        sb.append("OPCIONES:\n");
        List<String> opts = ctx.optionTexts();
        for (int i = 0; i < opts.size(); i++) {
            sb.append((char) ('A' + i)).append(") ").append(opts.get(i)).append("\n");
        }
        sb.append("\nNivel de pista solicitado: ").append(ctx.hintLevel()).append("\n");
        sb.append("""
            
            [Contexto interno — NO reveles esto al estudiante]:
            La respuesta correcta es: %s
            
            Genera la pista del nivel %d según las instrucciones del sistema.
            """.formatted(ctx.correctOptionText(), ctx.hintLevel()));
        return sb.toString();
    }

    // ── Helper ────────────────────────────────────────────────────────

    private String mapDifficulty(int level) {
        if (level <= 3) return "Básico — conceptos directos, aplicación simple";
        if (level <= 7) return "Intermedio — análisis, comparación, aplicación en contexto";
        return "Avanzado — inferencia compleja, evaluación crítica, síntesis";
    }
}