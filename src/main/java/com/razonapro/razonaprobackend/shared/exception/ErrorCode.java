package com.razonapro.razonaprobackend.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ── Autenticación / Autorización ──────────────────────────────────────
    INVALID_CREDENTIALS       ("AUTH-001", HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"),
    ACCOUNT_DISABLED          ("AUTH-002", HttpStatus.FORBIDDEN,    "Cuenta deshabilitada"),
    EMAIL_NOT_VERIFIED        ("AUTH-003", HttpStatus.FORBIDDEN,    "Debes verificar tu correo"),
    TOKEN_INVALID             ("AUTH-004", HttpStatus.BAD_REQUEST,  "Token inválido o expirado"),
    TOKEN_ALREADY_USED        ("AUTH-005", HttpStatus.BAD_REQUEST,  "Token ya utilizado"),
    INVALID_LOGIN_CODE        ("AUTH-006", HttpStatus.UNAUTHORIZED, "Formato de código inválido"),
    INSUFFICIENT_PERMS        ("AUTH-007", HttpStatus.FORBIDDEN,    "Sin permisos para esta acción"),
    TOO_MANY_REQUESTS         ("AUTH-008", HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos, espera un momento"),

    // ── Validación ────────────────────────────────────────────────────────
    VALIDATION_FAILED         ("VAL-001",  HttpStatus.BAD_REQUEST,  "Errores de validación"),
    INVALID_INPUT             ("VAL-002",  HttpStatus.BAD_REQUEST,  "Entrada inválida"),

    // ── Recursos ──────────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND        ("RES-001",  HttpStatus.NOT_FOUND,    "Recurso no encontrado"),

    // ── Conflictos ────────────────────────────────────────────────────────
    EMAIL_ALREADY_EXISTS      ("CFL-001",  HttpStatus.CONFLICT,     "El email ya está registrado"),
    CODE_ALREADY_EXISTS       ("CFL-002",  HttpStatus.CONFLICT,     "El código ya está registrado"),
    PHONE_ALREADY_EXISTS      ("CFL-003",  HttpStatus.CONFLICT,     "El teléfono ya está registrado"),
    DUPLICATE_RESOURCE        ("CFL-004",  HttpStatus.CONFLICT,     "Recurso duplicado"),

    // ── Negocio ───────────────────────────────────────────────────────────
    TEST_NO_QUESTIONS         ("BIZ-001",  HttpStatus.BAD_REQUEST,  "El test no tiene preguntas activas"),
    TRIED_ALREADY_FINISHED    ("BIZ-002",  HttpStatus.CONFLICT,     "El intento ya fue finalizado"),
    QUESTION_ALREADY_ANSWERED ("BIZ-003",  HttpStatus.CONFLICT,     "Ya respondiste esta pregunta"),
    PROGRAM_NOT_FOUND         ("BIZ-004",  HttpStatus.BAD_REQUEST,  "El código no corresponde a ningún programa registrado"),
    QUESTION_NO_CORRECT_OPTION("BIZ-005",  HttpStatus.BAD_REQUEST,  "Debe haber al menos una opción correcta"),
    TRIED_IN_PROGRESS         ("BIZ-006",  HttpStatus.CONFLICT,     "Ya tienes un intento en progreso para este test"),
    TEST_DISABLED             ("BIZ-007",  HttpStatus.BAD_REQUEST,  "El test no está disponible"),
    INVALID_OPTION            ("BIZ-008",  HttpStatus.BAD_REQUEST,  "Opción no válida para esta pregunta"),

    // ── Servidor ──────────────────────────────────────────────────────────
    INTERNAL_ERROR            ("SRV-001",  HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor"),
    EXTERNAL_SERVICE_DOWN     ("SRV-002",  HttpStatus.SERVICE_UNAVAILABLE,   "Servicio externo no disponible");

    private final String     code;
    private final HttpStatus status;
    private final String     defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}