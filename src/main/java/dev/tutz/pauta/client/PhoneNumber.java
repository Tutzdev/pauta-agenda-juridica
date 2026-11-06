package dev.tutz.pauta.client;

import dev.tutz.pauta.shared.BusinessRuleException;

/**
 * Telefone brasileiro com DDD, guardado só com dígitos ({@code 24999998888}).
 * Aceita o que a pessoa digitar: parênteses, traço, espaço e o +55 na frente.
 */
public record PhoneNumber(String digits) {

    private static final String COUNTRY_CODE = "55";

    public PhoneNumber {
        if (digits == null || !digits.matches("\\d{10,11}")) {
            throw new BusinessRuleException("INVALID_PHONE",
                    "Telefone inválido. Use o DDD e o número, por exemplo (24) 99999-8888.");
        }
    }

    public static PhoneNumber parse(String input) {
        String digits = input.replaceAll("\\D", "");
        if (digits.length() > 11 && digits.startsWith(COUNTRY_CODE)) {
            digits = digits.substring(COUNTRY_CODE.length());
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return new PhoneNumber(digits);
    }

    public boolean isMobile() {
        return digits.length() == 11 && digits.charAt(2) == '9';
    }
}
