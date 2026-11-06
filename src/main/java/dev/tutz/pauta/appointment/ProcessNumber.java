package dev.tutz.pauta.appointment;

import java.math.BigInteger;

import dev.tutz.pauta.shared.BusinessRuleException;

/**
 * Número único de processo no padrão do CNJ (Resolução 65/2008): {@code NNNNNNN-DD.AAAA.J.TR.OOOO}.
 * Os dígitos verificadores {@code DD} são conferidos (módulo 97), então um número digitado
 * errado é recusado na hora em vez de ir parar na agenda.
 */
public record ProcessNumber(String value) {

    private static final int       DIGITS = 20;
    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

    public ProcessNumber {
        if (value == null || !value.matches("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}")) {
            throw invalid();
        }
    }

    /** Aceita o número com ou sem pontuação. */
    public static ProcessNumber parse(String input) {
        String digits = input.replaceAll("\\D", "");
        if (digits.length() != DIGITS) {
            throw invalid();
        }
        String sequence = digits.substring(0, 7);
        String checkDigits = digits.substring(7, 9);
        String year = digits.substring(9, 13);
        String justice = digits.substring(13, 14);
        String court = digits.substring(14, 16);
        String origin = digits.substring(16, 20);

        BigInteger whole = new BigInteger(sequence + year + justice + court + origin + checkDigits);
        if (whole.mod(NINETY_SEVEN).intValue() != 1) {
            throw new BusinessRuleException("INVALID_PROCESS_NUMBER",
                    "Os dígitos verificadores do número do processo não conferem. Confira o número.");
        }
        return new ProcessNumber("%s-%s.%s.%s.%s.%s".formatted(sequence, checkDigits, year, justice, court, origin));
    }

    private static BusinessRuleException invalid() {
        return new BusinessRuleException("INVALID_PROCESS_NUMBER",
                "Número de processo inválido. Use o padrão do CNJ: 0000000-00.0000.0.00.0000.");
    }

    @Override
    public String toString() {
        return value;
    }
}
