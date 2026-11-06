package dev.tutz.pauta.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AlertClassifierTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private final AlertClassifier classifier = new AlertClassifier(new AlertProperties(5));

    @Test
    void anOpenDeadlineInThePastIsOverdue() {
        assertThat(alertOf(AppointmentType.DEADLINE, TODAY.minusDays(1))).isEqualTo(Alert.OVERDUE);
    }

    @Test
    void aForgottenHearingInThePastIsAlsoOverdue() {
        assertThat(alertOf(AppointmentType.HEARING, TODAY.minusDays(3))).isEqualTo(Alert.OVERDUE);
    }

    @Test
    void anythingOnTheReferenceDateIsToday() {
        assertThat(alertOf(AppointmentType.DEADLINE, TODAY)).isEqualTo(Alert.TODAY);
        assertThat(alertOf(AppointmentType.CLIENT_SERVICE, TODAY)).isEqualTo(Alert.TODAY);
    }

    @Test
    void deadlinesInsideTheWindowAreUpcomingAndTheLastDayOfTheWindowCounts() {
        assertThat(alertOf(AppointmentType.DEADLINE, TODAY.plusDays(1))).isEqualTo(Alert.UPCOMING);
        assertThat(alertOf(AppointmentType.DEADLINE, TODAY.plusDays(5))).isEqualTo(Alert.UPCOMING);
        assertThat(alertOf(AppointmentType.DEADLINE, TODAY.plusDays(6))).isEqualTo(Alert.NONE);
    }

    @Test
    void onlyDeadlinesGetTheUpcomingAlert() {
        assertThat(alertOf(AppointmentType.HEARING, TODAY.plusDays(2))).isEqualTo(Alert.NONE);
    }

    @Test
    void doneOrCanceledNeverAlert() {
        Appointment done = Appointments.of(AppointmentType.DEADLINE, TODAY.minusDays(1));
        done.complete(Appointments.NOW);
        Appointment canceled = Appointments.of(AppointmentType.DEADLINE, TODAY);
        canceled.cancel(Appointments.NOW);

        assertThat(classifier.classify(done, TODAY)).isEqualTo(Alert.NONE);
        assertThat(classifier.classify(canceled, TODAY)).isEqualTo(Alert.NONE);
    }

    private Alert alertOf(AppointmentType type, LocalDate date) {
        return classifier.classify(Appointments.of(type, date), TODAY);
    }
}
