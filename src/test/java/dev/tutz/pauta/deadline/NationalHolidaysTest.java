package dev.tutz.pauta.deadline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class NationalHolidaysTest {

    @ParameterizedTest
    @CsvSource({"2024, 2024-03-31", "2025, 2025-04-20", "2026, 2026-04-05", "2027, 2027-03-28", "2038, 2038-04-25"})
    void easterSundayMatchesTheGregorianCalendar(int year, LocalDate expected) {
        assertThat(NationalHolidays.easterSunday(year)).isEqualTo(expected);
    }

    @Test
    void movableHolidaysFollowEaster() {
        var holidays = NationalHolidays.of(2026);

        assertThat(holidays).containsEntry(LocalDate.of(2026, 2, 16), "Carnaval")
                .containsEntry(LocalDate.of(2026, 2, 17), "Carnaval")
                .containsEntry(LocalDate.of(2026, 4, 3), "Sexta-feira Santa")
                .containsEntry(LocalDate.of(2026, 6, 4), "Corpus Christi");
    }

    @Test
    void consciousnessDayIsANationalHolidaySince2024() {
        assertThat(NationalHolidays.of(2026)).containsEntry(LocalDate.of(2026, 11, 20), "Dia da Consciência Negra");
    }
}
