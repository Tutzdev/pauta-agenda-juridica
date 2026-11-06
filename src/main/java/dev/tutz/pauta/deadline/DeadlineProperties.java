package dev.tutz.pauta.deadline;

import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ajustes do calendário forense.
 *
 * @param localHolidays feriados estaduais e municipais no formato {@code MM-dd} (ex.: {@code 07-17})
 * @param courtRecess   se o recesso de 20/12 a 20/01 (CPC, art. 220) suspende a contagem
 */
@ConfigurationProperties("pauta.deadlines")
public record DeadlineProperties(List<String> localHolidays, boolean courtRecess) {

    public DeadlineProperties {
        localHolidays = localHolidays == null ? List.of() : List.copyOf(localHolidays);
        localHolidayDates(localHolidays);
    }

    public Set<MonthDay> localHolidayDates() {
        return localHolidayDates(localHolidays);
    }

    private static Set<MonthDay> localHolidayDates(List<String> values) {
        return values.stream()
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(DeadlineProperties::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static MonthDay parse(String value) {
        try {
            return MonthDay.parse("--" + value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "pauta.deadlines.local-holidays: \"" + value + "\" não está no formato MM-dd.", exception);
        }
    }
}
