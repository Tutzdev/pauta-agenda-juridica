package dev.tutz.pauta.appointment;

import static dev.tutz.pauta.appointment.Appointments.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import dev.tutz.pauta.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

class AppointmentTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);
    private static final Instant LATER = NOW.plusSeconds(3600);

    @Test
    void startsOpen() {
        assertThat(Appointments.of(AppointmentType.DEADLINE, DATE).getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void completingRecordsWhenAndReopeningClearsIt() {
        Appointment appointment = Appointments.of(AppointmentType.DEADLINE, DATE);

        appointment.complete(LATER);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.DONE);
        assertThat(appointment.getCompletedAt()).isEqualTo(LATER);

        appointment.reopen(LATER);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.getCompletedAt()).isNull();
    }

    @Test
    void aCanceledAppointmentCannotBeMarkedAsDone() {
        Appointment appointment = Appointments.of(AppointmentType.MEETING, DATE);
        appointment.cancel(LATER);

        assertThatThrownBy(() -> appointment.complete(LATER))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void reopeningAnOpenAppointmentIsRejected() {
        Appointment appointment = Appointments.of(AppointmentType.MEETING, DATE);

        assertThatThrownBy(() -> appointment.reopen(LATER)).hasMessageContaining("já está em aberto");
    }

    @Test
    void aTimeWithoutDurationTakesOneHour() {
        Appointment appointment = Appointments.at(DATE, LocalTime.of(10, 0, 30), 60);
        Appointment noDuration = new Appointment(
                Appointments.details(AppointmentType.HEARING, DATE, LocalTime.of(10, 0), null), null, NOW);

        assertThat(appointment.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(noDuration.getDurationMinutes()).isEqualTo(60);
    }

    @Test
    void anAllDayAppointmentHasNoDurationEvenIfOneIsSent() {
        Appointment appointment = new Appointment(
                Appointments.details(AppointmentType.DEADLINE, DATE, null, 90), null, NOW);

        assertThat(appointment.getDurationMinutes()).isNull();
        assertThat(appointment.slot()).isNull();
    }
}
