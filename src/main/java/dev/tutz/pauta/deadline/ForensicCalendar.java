package dev.tutz.pauta.deadline;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Diz se um dia conta para prazo processual e, quando não conta, por quê. */
@Component
public class ForensicCalendar {

    private static final MonthDay RECESS_START = MonthDay.of(Month.DECEMBER, 20);
    private static final MonthDay RECESS_END   = MonthDay.of(Month.JANUARY, 20);

    private final Set<MonthDay>                        localHolidays;
    private final boolean                              courtRecess;
    private final Map<Integer, Map<LocalDate, String>> nationalByYear = new ConcurrentHashMap<>();

    public ForensicCalendar(DeadlineProperties properties) {
        this.localHolidays = properties.localHolidayDates();
        this.courtRecess   = properties.courtRecess();
    }

    public boolean isBusinessDay(LocalDate date) {
        return closureReason(date).isEmpty();
    }

    /** Motivo de o dia não contar: fim de semana, feriado ou recesso. Vazio quando é dia útil. */
    public Optional<String> closureReason(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return Optional.of("Sábado");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return Optional.of("Domingo");
        }
        if (courtRecess && inRecess(date)) {
            return Optional.of("Recesso forense");
        }
        String holiday = nationalByYear.computeIfAbsent(date.getYear(), NationalHolidays::of).get(date);
        if (holiday != null) {
            return Optional.of(holiday);
        }
        if (localHolidays.contains(MonthDay.from(date))) {
            return Optional.of("Feriado local");
        }
        return Optional.empty();
    }

    /** CPC, art. 220: suspende-se o curso do prazo entre 20 de dezembro e 20 de janeiro, inclusive. */
    private static boolean inRecess(LocalDate date) {
        MonthDay day = MonthDay.from(date);
        return !day.isBefore(RECESS_START) || !day.isAfter(RECESS_END);
    }

    static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
