package dev.tutz.pauta.deadline;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dias sem expediente forense em todo o país: feriados nacionais (Lei 662/1949 e Lei 14.759/2023)
 * e os móveis que os tribunais seguem (Carnaval, Sexta-feira Santa e Corpus Christi).
 */
final class NationalHolidays {

    private NationalHolidays() {
    }

    static Map<LocalDate, String> of(int year) {
        Map<LocalDate, String> holidays = new TreeMap<>();
        holidays.put(LocalDate.of(year, Month.JANUARY, 1), "Confraternização Universal");
        holidays.put(LocalDate.of(year, Month.APRIL, 21), "Tiradentes");
        holidays.put(LocalDate.of(year, Month.MAY, 1), "Dia do Trabalho");
        holidays.put(LocalDate.of(year, Month.SEPTEMBER, 7), "Independência do Brasil");
        holidays.put(LocalDate.of(year, Month.OCTOBER, 12), "Nossa Senhora Aparecida");
        holidays.put(LocalDate.of(year, Month.NOVEMBER, 2), "Finados");
        holidays.put(LocalDate.of(year, Month.NOVEMBER, 15), "Proclamação da República");
        holidays.put(LocalDate.of(year, Month.NOVEMBER, 20), "Dia da Consciência Negra");
        holidays.put(LocalDate.of(year, Month.DECEMBER, 25), "Natal");

        LocalDate easter = easterSunday(year);
        holidays.put(easter.minusDays(48), "Carnaval");
        holidays.put(easter.minusDays(47), "Carnaval");
        holidays.put(easter.minusDays(2), "Sexta-feira Santa");
        holidays.put(easter.plusDays(60), "Corpus Christi");
        return Map.copyOf(holidays);
    }

    /** Domingo de Páscoa pelo algoritmo de Meeus/Jones/Butcher (calendário gregoriano). */
    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }
}
