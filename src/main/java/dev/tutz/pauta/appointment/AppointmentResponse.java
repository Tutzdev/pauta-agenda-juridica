package dev.tutz.pauta.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record AppointmentResponse(
        Long id,
        String title,
        AppointmentType type,
        AppointmentStatus status,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Integer durationMinutes,
        String location,
        String processNumber,
        ClientSummary client,
        String notes,
        Alert alert,
        Instant completedAt) {

    public record ClientSummary(Long id, String name, String phone) {
    }
}
