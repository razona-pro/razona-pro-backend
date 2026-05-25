package com.razonapro.razonaprobackend.infrastructure.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {}

    /** AMN001 … AMN999 */
    public static String adminId(long count) {
        return String.format("AMN%03d", count + 1);
    }

    /** CPE001 … CPE999 */
    public static String competenceId(long count) {
        return String.format("CPE%03d", count + 1);
    }

    /** RKG001 … RKG999 */
    public static String rankingId(long count) {
        return String.format("RKG%03d", count + 1);
    }

    /** QTN0001 … QTN9999 (7 chars, por competencia) */
    public static String questionId(long count) {
        return String.format("QTN%04d", count + 1);
    }

    /** OTN001 … OTN999 (por pregunta) */
    public static String optionId(long count) {
        return String.format("OTN%03d", count + 1);
    }

    /** TET00001 … TET99999 (8 chars) */
    public static String testId(long count) {
        return String.format("TET%05d", count + 1);
    }

    /** TRD + 7 random = 10 chars */
    public static String triedId() {
        return "TRD" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }

    /** SRE + 7 random = 10 chars */
    public static String studentResponseId() {
        return "SRE" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }

    /** ATD + 7 random = 10 chars */
    public static String aiTriedId() {
        return "ATD" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }

    /** ATE + 7 random = 10 chars */
    public static String aiTriedResponseId() {
        return "ATE" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }
}