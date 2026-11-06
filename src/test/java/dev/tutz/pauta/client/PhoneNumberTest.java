package dev.tutz.pauta.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.tutz.pauta.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"(24) 99812-3344", "24998123344", "+55 24 99812-3344", "024 99812 3344"})
    void keepsOnlyTheAreaCodeAndNumber(String input) {
        assertThat(PhoneNumber.parse(input).digits()).isEqualTo("24998123344");
    }

    @Test
    void tellsMobileFromLandline() {
        assertThat(PhoneNumber.parse("(24) 99812-3344").isMobile()).isTrue();
        assertThat(PhoneNumber.parse("(24) 3342-1180").isMobile()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"99812-3344", "abc", "2499812334455"})
    void rejectsNumbersWithoutAreaCodeOrTooLong(String input) {
        assertThatThrownBy(() -> PhoneNumber.parse(input))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_PHONE");
    }
}
