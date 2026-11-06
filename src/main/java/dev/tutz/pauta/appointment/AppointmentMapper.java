package dev.tutz.pauta.appointment;

import java.time.LocalDate;

import dev.tutz.pauta.client.Client;
import org.springframework.stereotype.Component;

@Component
class AppointmentMapper {

    private final AlertClassifier classifier;

    AppointmentMapper(AlertClassifier classifier) {
        this.classifier = classifier;
    }

    AppointmentResponse toResponse(Appointment appointment, LocalDate today) {
        TimeSlot slot = appointment.slot();
        Client client = appointment.getClient();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getTitle(),
                appointment.getType(),
                appointment.getStatus(),
                appointment.getDate(),
                appointment.getStartTime(),
                slot == null ? null : slot.end(),
                appointment.getDurationMinutes(),
                appointment.getLocation(),
                appointment.getProcessNumber(),
                client == null ? null
                        : new AppointmentResponse.ClientSummary(client.getId(), client.getName(), client.getPhone()),
                appointment.getNotes(),
                classifier.classify(appointment, today),
                appointment.getCompletedAt());
    }

    ScheduleConflict toConflict(Appointment appointment) {
        return new ScheduleConflict(appointment.getId(), appointment.getTitle(), appointment.getType(),
                appointment.getStartTime(), appointment.slot().end());
    }
}
