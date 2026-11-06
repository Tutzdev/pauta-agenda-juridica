package dev.tutz.pauta.appointment;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

/**
 * Decide o alerta de cada compromisso em relação a uma data de referência (o "hoje").
 * Recebe a data em vez de ler o relógio, então a tela inteira é classificada com o mesmo "hoje".
 */
@Component
public class AlertClassifier {

    private final int upcomingDays;

    public AlertClassifier(AlertProperties properties) {
        this.upcomingDays = properties.upcomingDays();
    }

    public Alert classify(Appointment appointment, LocalDate today) {
        if (!appointment.isOpen()) {
            return Alert.NONE;
        }
        LocalDate date = appointment.getDate();
        if (date.isBefore(today)) {
            return Alert.OVERDUE;
        }
        if (date.isEqual(today)) {
            return Alert.TODAY;
        }
        boolean deadline = appointment.getType() == AppointmentType.DEADLINE;
        if (deadline && !date.isAfter(today.plusDays(upcomingDays))) {
            return Alert.UPCOMING;
        }
        return Alert.NONE;
    }

    public int upcomingDays() {
        return upcomingDays;
    }
}
