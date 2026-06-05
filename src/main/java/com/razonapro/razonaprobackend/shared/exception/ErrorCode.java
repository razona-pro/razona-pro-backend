package com.razonapro.razonaprobackend.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de errores de negocio de la aplicación.
 * <p>
 * El "código" que viaja al cliente es el NOMBRE del enum ({@link #name()}),
 * que es estable, semántico y único por construcción — no se mantienen
 * identificadores arbitrarios. Cada error declara su estado HTTP y un mensaje
 * por defecto en español.
 */
public enum ErrorCode {

    // ── Autenticación / Autorización (401 / 403) ──────────────────────────
    UNAUTHENTICATED           (HttpStatus.UNAUTHORIZED,      "Debes iniciar sesión para continuar."),
    INVALID_CREDENTIALS       (HttpStatus.UNAUTHORIZED,      "Credenciales incorrectas."),
    INVALID_LOGIN_CODE        (HttpStatus.UNAUTHORIZED,      "El formato del código de acceso es inválido."),
    ACCOUNT_DISABLED          (HttpStatus.FORBIDDEN,         "Tu cuenta está deshabilitada."),
    EMAIL_NOT_VERIFIED        (HttpStatus.FORBIDDEN,         "Debes verificar tu correo antes de continuar."),
    EMAIL_ALREADY_VERIFIED    (HttpStatus.CONFLICT,          "Tu correo ya está verificado."),
    INSUFFICIENT_PERMS        (HttpStatus.FORBIDDEN,         "No tienes permisos para esta acción."),
    TOKEN_INVALID             (HttpStatus.BAD_REQUEST,       "El enlace es inválido o expiró."),
    TOKEN_ALREADY_USED        (HttpStatus.BAD_REQUEST,       "Este enlace ya fue utilizado."),
    TOO_MANY_REQUESTS         (HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera un momento."),

    // ── Validación (400) ──────────────────────────────────────────────────
    VALIDATION_FAILED         (HttpStatus.BAD_REQUEST,       "Hay errores de validación."),
    INVALID_INPUT             (HttpStatus.BAD_REQUEST,       "La información enviada no es válida."),
    MALFORMED_REQUEST         (HttpStatus.BAD_REQUEST,       "La solicitud está mal formada."),
    METHOD_NOT_ALLOWED        (HttpStatus.METHOD_NOT_ALLOWED,"Método no permitido para este recurso."),

    // ── Recursos (404) ────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND        (HttpStatus.NOT_FOUND,         "Recurso no encontrado."),
    ENDPOINT_NOT_FOUND        (HttpStatus.NOT_FOUND,         "La ruta solicitada no existe."),

    // ── Conflictos (409) ──────────────────────────────────────────────────
    EMAIL_ALREADY_EXISTS      (HttpStatus.CONFLICT,          "El correo ya está registrado."),
    CODE_ALREADY_EXISTS       (HttpStatus.CONFLICT,          "El código ya está registrado."),
    PHONE_ALREADY_EXISTS      (HttpStatus.CONFLICT,          "El teléfono ya está registrado."),
    DUPLICATE_RESOURCE        (HttpStatus.CONFLICT,          "El recurso ya existe."),
    DATA_INTEGRITY_VIOLATION  (HttpStatus.CONFLICT,          "La operación viola una restricción de datos."),

    // ── Negocio (4xx) ─────────────────────────────────────────────────────
    PROGRAM_NOT_FOUND         (HttpStatus.BAD_REQUEST,       "El código no corresponde a ningún programa registrado."),
    TEST_NO_QUESTIONS         (HttpStatus.BAD_REQUEST,       "El test no tiene preguntas activas."),
    TEST_DISABLED             (HttpStatus.BAD_REQUEST,       "El test no está disponible."),
    QUESTION_NO_CORRECT_OPTION(HttpStatus.BAD_REQUEST,       "Debe haber al menos una opción correcta."),
    INVALID_OPTION            (HttpStatus.BAD_REQUEST,       "Opción no válida para esta pregunta."),
    TRIED_NOT_FINISHED        (HttpStatus.BAD_REQUEST,       "El intento aún no ha finalizado."),
    TRIED_ALREADY_FINISHED    (HttpStatus.CONFLICT,          "El intento ya fue finalizado."),
    TRIED_IN_PROGRESS         (HttpStatus.CONFLICT,          "Ya tienes un intento en progreso para este test."),
    QUESTION_ALREADY_ANSWERED (HttpStatus.CONFLICT,          "Ya respondiste esta pregunta."),
    RESEND_TOO_SOON           (HttpStatus.TOO_MANY_REQUESTS, "Espera un momento antes de reenviar el correo."),

    // ── Módulo de IA (4xx / 503) ──────────────────────────────────────────
    AI_MODULE_DISABLED        (HttpStatus.SERVICE_UNAVAILABLE, "El módulo de IA no está habilitado. Contacta al administrador."),
    AI_TUTOR_DISABLED         (HttpStatus.SERVICE_UNAVAILABLE, "El tutor IA no está disponible actualmente."),
    AI_GENERATION_FAILED      (HttpStatus.SERVICE_UNAVAILABLE, "Error al generar la pregunta con IA. Intenta nuevamente."),
    AI_BATCH_EMPTY            (HttpStatus.SERVICE_UNAVAILABLE, "La IA no devolvió preguntas válidas. Intenta de nuevo."),
    AI_SESSION_NOT_FOUND      (HttpStatus.NOT_FOUND,           "Sesión de IA no encontrada o expirada. Inicia una nueva práctica."),
    AI_QUESTION_NOT_FOUND     (HttpStatus.NOT_FOUND,           "Pregunta IA no encontrada en esta sesión."),
    AI_QUESTION_NOT_MATCH     (HttpStatus.BAD_REQUEST,         "La pregunta no corresponde a la sesión activa."),
    AI_INVALID_OPTION         (HttpStatus.BAD_REQUEST,         "Opción de respuesta inválida."),
    AI_ALREADY_ANSWERED       (HttpStatus.CONFLICT,            "Esta pregunta ya fue respondida."),
    HINT_LIMIT_REACHED        (HttpStatus.CONFLICT,            "Ya alcanzaste el máximo de pistas para esta pregunta."),

    // ── Otros recursos (404) ──────────────────────────────────────────────
    DOUBT_NOT_FOUND           (HttpStatus.NOT_FOUND,         "Reporte de duda no encontrado."),
    NOTIFICATION_NOT_FOUND    (HttpStatus.NOT_FOUND,         "Notificación no encontrada."),

    // ── Servidor (5xx) ────────────────────────────────────────────────────
    INTERNAL_ERROR            (HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Inténtalo más tarde."),
    EXTERNAL_SERVICE_DOWN     (HttpStatus.SERVICE_UNAVAILABLE,   "Un servicio externo no está disponible.");

    private final HttpStatus status;
    private final String     defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    /** Código estable y semántico que viaja al cliente (el nombre del enum). */
    public String getCode() { return name(); }

    public HttpStatus getStatus() { return status; }

    public String getDefaultMessage() { return defaultMessage; }
}
