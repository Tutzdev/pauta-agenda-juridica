package dev.tutz.pauta.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.tutz.pauta.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProcessNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"0801234-67.2026.8.19.0066", "08012346720268190066", " 0801234 67 2026 8 19 0066 "})
    void acceptsTheNumberWithOrWithoutPunctuationAndFormatsIt(String input) {
        assertThat(ProcessNumber.parse(input).value()).isEqualTo("0801234-67.2026.8.19.0066");
    }

    @Test
    void acceptsLabourCourtNumbers() {
        assertThat(ProcessNumber.parse("0100321-64.2025.5.01.0341").value()).isEqualTo("0100321-64.2025.5.01.0341");
    }

    @Test
    void rejectsATypoThatBreaksTheCheckDigits() {
        assertThatThrownBy(() -> ProcessNumber.parse("0801234-67.2026.8.19.0067"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dígitos verificadores")
                .extracting("code").isEqualTo("INVALID_PROCESS_NUMBER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "0801234-67.2026.8.19", "0801234-67.2026.8.19.00661"})
    void rejectsNumbersWithTheWrongLength(String input) {
        assertThatThrownBy(() -> ProcessNumber.parse(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("padrão do CNJ");
    }
}
