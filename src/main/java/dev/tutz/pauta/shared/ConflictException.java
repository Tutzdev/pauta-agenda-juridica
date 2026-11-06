package dev.tutz.pauta.shared;

/** A operação conflita com dados existentes (responde 409). */
public class ConflictException extends BusinessRuleException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
