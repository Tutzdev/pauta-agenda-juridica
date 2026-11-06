package dev.tutz.pauta.appointment;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Outro compromisso em aberto que ocupa o mesmo horário. É um aviso: a advogada decide se mantém. */
public record ScheduleConflict(
        Long id,
        String title,
        AppointmentType type,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
}
