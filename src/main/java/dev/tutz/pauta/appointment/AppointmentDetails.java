package dev.tutz.pauta.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import dev.tutz.pauta.shared.Text;

/**
 * Os dados editáveis de um compromisso, já normalizados. Reunidos aqui para a entidade
 * não receber nove parâmetros soltos na criação e na edição.
 */
record AppointmentDetails(String title, AppointmentType type, LocalDate date, LocalTime startTime,
                          Integer durationMinutes, String location, ProcessNumber processNumber, String notes) {

    static final int DEFAULT_DURATION_MINUTES = 60;

    AppointmentDetails {
        title = Objects.requireNonNull(Text.trimToNull(title), "title");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(date, "date");
        if (startTime == null) {
            durationMinutes = null;
        } else {
            startTime = startTime.withSecond(0).withNano(0);
            durationMinutes = durationMinutes == null ? DEFAULT_DURATION_MINUTES : durationMinutes;
        }
        location = Text.trimToNull(location);
        notes = Text.trimToNull(notes);
    }

    TimeSlot slot() {
        return startTime == null ? null : TimeSlot.of(startTime, durationMinutes);
    }
}
