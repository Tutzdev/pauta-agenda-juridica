package dev.tutz.pauta.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Fábrica de compromissos para os testes. */
final class Appointments {

    static final Instant NOW = Instant.parse("2026-11-06T13:00:00Z");

    private Appointments() {
    }

    static Appointment of(AppointmentType type, LocalDate date) {
        return new Appointment(details(type, date, null, null), null, NOW);
    }

    static Appointment at(LocalDate date, LocalTime start, int durationMinutes) {
        return new Appointment(details(AppointmentType.HEARING, date, start, durationMinutes), null, NOW);
    }

    static AppointmentDetails details(AppointmentType type, LocalDate date, LocalTime start, Integer duration) {
        return new AppointmentDetails("Compromisso", type, date, start, duration, null, null, null);
    }
}
