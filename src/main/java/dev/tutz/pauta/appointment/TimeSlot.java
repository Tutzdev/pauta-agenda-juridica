package dev.tutz.pauta.appointment;

import java.time.LocalTime;

/** Intervalo de um compromisso dentro do dia, em minutos desde a meia-noite. Termina no máximo às 24h. */
record TimeSlot(int startMinute, int endMinute) {

    private static final int END_OF_DAY = 24 * 60;

    TimeSlot {
        if (startMinute < 0 || endMinute > END_OF_DAY || endMinute <= startMinute) {
            throw new IllegalArgumentException("Intervalo inválido: " + startMinute + " a " + endMinute);
        }
    }

    static TimeSlot of(LocalTime start, int durationMinutes) {
        int startMinute = start.getHour() * 60 + start.getMinute();
        return new TimeSlot(startMinute, Math.min(startMinute + durationMinutes, END_OF_DAY));
    }

    /** Encostar não é sobrepor: 14h–15h e 15h–16h não conflitam. */
    boolean overlaps(TimeSlot other) {
        return startMinute < other.endMinute && other.startMinute < endMinute;
    }

    LocalTime end() {
        return endMinute == END_OF_DAY ? LocalTime.of(23, 59) : LocalTime.of(endMinute / 60, endMinute % 60);
    }
}
