package com.razonapro.razonaprobackend.infrastructure.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {}

    /** ADM001 … ADM999 */
    public static String adminId(long count) {
        return String.format("ADM%03d", count + 1);
    }

    /** SIS, ELE, IND — se pasa manual al crear programa */
    public static String programId(String prefix) {
        return prefix.toUpperCase().substring(0, Math.min(3, prefix.length()));
    }

    /** COM001 … COM999 */
    public static String competenceId(long count) {
        return String.format("COM%03d", count + 1);
    }

    /** STU0001 … STU9999 */
    public static String studentId(long count) {
        return String.format("STU%04d", count + 1);
    }

    /** RNK001 … RNK999 */
    public static String rankingId(long count) {
        return String.format("RNK%03d", count + 1);
    }

    /** QST001 … QST999 (por competencia) */
    public static String questionId(long count) {
        return String.format("QST%03d", count + 1);
    }

    /** OPT001 … OPT999 (por pregunta) */
    public static String optionId(long count) {
        return String.format("OPT%03d", count + 1);
    }

    /** TST00001 … (8 chars) */
    public static String testId(long count) {
        return String.format("TST%05d", count + 1);
    }

    /** TR + 8 hex random = 10 chars */
    public static String triedId() {
        return "TR" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /** SR + 8 hex random = 10 chars */
    public static String studentResponseId() {
        return "SR" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /** AI + 8 hex random = 10 chars */
    public static String aiTriedId() {
        return "AI" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /** AR + 8 hex random = 10 chars */
    public static String aiTriedResponseId() {
        return "AR" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
