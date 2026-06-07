package com.razonapro.razonaprobackend.infrastructure.util;

public final class IdGenerator {

    private IdGenerator() {}

    /** Opción centinela "Sin responder" - nunca se muestra al estudiante */
    public static final String UNANSWERED_OPTION_ID = "OTN000";

    public static String adminId(long count)      { return String.format("AMN%03d", count + 1); }
    public static String competenceId(long count) { return String.format("CPE%03d", count + 1); }
    public static String rankingId(long count)    { return String.format("RKG%03d", count + 1); }
    public static String questionId(long count)   { return String.format("QTN%04d", count + 1); }
    public static String optionId(long count)     { return String.format("OTN%03d", count + 1); }
    public static String testId(long count)       { return String.format("TET%05d", count + 1); }

    // Numeración secuencial (igual que admins/preguntas) - no UUID.
    public static String triedId(long count)           { return String.format("TRD%07d", count + 1); }
    public static String studentResponseId(long count) { return String.format("SRE%07d", count + 1); }
    public static String aiTriedId(long count)         { return String.format("ATD%07d", count + 1); }
    public static String aiTriedResponseId(long count) { return String.format("ATE%07d", count + 1); }
    public static String aiQuestionId(long count)      { return String.format("AQN%09d", count + 1); }
    public static String notificationId(long count)    { return String.format("NOT%09d", count + 1); }
    public static String doubtId(long count)           { return String.format("DBT%09d", count + 1); }
    public static String appealId(long count)          { return String.format("APL%09d", count + 1); }
}