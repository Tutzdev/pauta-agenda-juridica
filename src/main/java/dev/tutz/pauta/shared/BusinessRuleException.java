package dev.tutz.pauta.shared;

/** Regra de negócio violada. O {@code code} é estável e o front-end pode reagir a ele. */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
