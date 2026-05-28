package com.razonapro.razonaprobackend.infrastructure.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {}

    /** Opción centinela "Sin responder" — nunca se muestra al estudiante */
    public static final String UNANSWERED_OPTION_ID = "OTN000";

    public static String adminId(long count)      { return String.format("AMN%03d", count + 1); }
    public static String competenceId(long count) { return String.format("CPE%03d", count + 1); }
    public static String rankingId(long count)    { return String.format("RKG%03d", count + 1); }
    public static String questionId(long count)   { return String.format("QTN%04d", count + 1); }
    public static String optionId(long count)     { return String.format("OTN%03d", count + 1); }
    public static String testId(long count)       { return String.format("TET%05d", count + 1); }

    public static String triedId()           { return "TRD" + uuid7(); }
    public static String studentResponseId() { return "SRE" + uuid7(); }
    public static String aiTriedId()         { return "ATD" + uuid7(); }
    public static String aiTriedResponseId() { return "ATE" + uuid7(); }

    private static String uuid7() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }
}