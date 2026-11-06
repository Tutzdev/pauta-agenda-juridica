package dev.tutz.pauta.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import dev.tutz.pauta.deadline.DeadlineCalculation.SkippedPeriod;
import dev.tutz.pauta.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeadlineCalculatorTest {

    private final DeadlineCalculator calculator = calculator(List.of("07-17"), true);

    @Test
    void theStartDayDoesNotCountAndAFridayIntimationStartsOnMonday() {
        DeadlineCalculation result = calculator.calculate(LocalDate.of(2026, 11, 6), 1);

        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(result.weekendDays()).isEqualTo(2);
    }

    @Test
    void anIntimationOnSaturdayStillStartsCountingOnMonday() {
        assertThat(calculator.calculate(LocalDate.of(2026, 11, 7), 1).dueDate()).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void fifteenBusinessDaysSkipWeekendsAndNationalHolidays() {
        DeadlineCalculation result = calculator.calculate(LocalDate.of(2026, 11, 6), 15);

        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(result.skipped()).containsExactly(
                new SkippedPeriod(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20), "Dia da Consciência Negra"));
    }

    @Test
    void courtRecessSuspendsTheCountAndIsReportedAsASinglePeriod() {
        DeadlineCalculation result = calculator.calculate(LocalDate.of(2026, 12, 15), 5);

        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2027, 1, 22));
        assertThat(result.skipped()).containsExactly(
                new SkippedPeriod(LocalDate.of(2026, 12, 21), LocalDate.of(2027, 1, 20), "Recesso forense"));
    }

    @Test
    void withoutTheRecessOnlyHolidaysAreSkippedInDecember() {
        DeadlineCalculation result = calculator(List.of(), false).calculate(LocalDate.of(2026, 12, 22), 3);

        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(result.skipped()).extracting(SkippedPeriod::reason).containsExactly("Natal");
    }

    @Test
    void configuredLocalHolidayIsSkipped() {
        DeadlineCalculation result = calculator.calculate(LocalDate.of(2026, 7, 16), 1);

        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(result.skipped()).extracting(SkippedPeriod::reason).containsExactly("Feriado local");
    }

    @Test
    void consecutiveClosuresWithDifferentReasonsAreReportedSeparately() {
        DeadlineCalculation result = calculator(List.of("11-03"), false).calculate(LocalDate.of(2026, 10, 30), 3);

        assertThat(result.skipped()).extracting(SkippedPeriod::reason).containsExactly("Finados", "Feriado local");
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 366})
    void rejectsDeadlinesOutsideTheSupportedLength(int businessDays) {
        assertThatThrownBy(() -> calculator.calculate(LocalDate.of(2026, 11, 6), businessDays))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_DEADLINE_LENGTH");
    }

    @Test
    void rejectsLocalHolidaysInTheWrongFormat() {
        assertThatThrownBy(() -> new DeadlineProperties(List.of("17/07"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MM-dd");
    }

    private static DeadlineCalculator calculator(List<String> localHolidays, boolean recess) {
        return new DeadlineCalculator(new ForensicCalendar(new DeadlineProperties(localHolidays, recess)));
    }
}
