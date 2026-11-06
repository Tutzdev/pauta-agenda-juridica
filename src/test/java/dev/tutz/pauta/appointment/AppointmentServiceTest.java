package dev.tutz.pauta.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import dev.tutz.pauta.client.ClientService;
import dev.tutz.pauta.shared.ApplicationConfiguration;
import dev.tutz.pauta.shared.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    @Mock
    private AppointmentRepository repository;

    @Mock
    private ClientService clientService;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Appointments.NOW, ApplicationConfiguration.BUSINESS_ZONE);
        AppointmentMapper mapper = new AppointmentMapper(new AlertClassifier(new AlertProperties(5)));
        service = new AppointmentService(repository, clientService, mapper, clock);
    }

    @Test
    void conflictsListOnlyOpenAppointmentsWhoseTimeOverlaps() {
        Appointment hearing = withId(Appointments.at(DAY, LocalTime.of(14, 0), 60), 1L);
        Appointment later = withId(Appointments.at(DAY, LocalTime.of(16, 0), 60), 2L);
        when(repository.findByDateAndStatusAndStartTimeNotNull(DAY, AppointmentStatus.SCHEDULED))
                .thenReturn(List.of(later, hearing));

        List<ScheduleConflict> conflicts = service.conflicts(DAY, LocalTime.of(14, 30), 30, null);

        assertThat(conflicts).extracting(ScheduleConflict::id).containsExactly(1L);
        assertThat(conflicts.getFirst().endTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    void whileEditingAnAppointmentItDoesNotConflictWithItself() {
        Appointment hearing = withId(Appointments.at(DAY, LocalTime.of(14, 0), 60), 1L);
        when(repository.findByDateAndStatusAndStartTimeNotNull(DAY, AppointmentStatus.SCHEDULED))
                .thenReturn(List.of(hearing));

        assertThat(service.conflicts(DAY, LocalTime.of(14, 0), 60, 1L)).isEmpty();
    }

    @Test
    void aRangeLongerThanThreeMonthsIsRejectedBeforeQuerying() {
        assertThatThrownBy(() -> service.between(DAY, DAY.plusDays(93)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_RANGE");
        verify(repository, never()).findByDateBetween(any(), any());
    }

    @Test
    void anInvertedRangeIsRejected() {
        assertThatThrownBy(() -> service.between(DAY, DAY.minusDays(1))).hasMessageContaining("depois da final");
    }

    @Test
    void listsAllDayItemsBeforeTimedOnesOnTheSameDay() {
        Appointment morning = withId(Appointments.at(DAY, LocalTime.of(9, 0), 60), 1L);
        Appointment deadline = withId(Appointments.of(AppointmentType.DEADLINE, DAY), 2L);
        Appointment nextDay = withId(Appointments.of(AppointmentType.DEADLINE, DAY.plusDays(1)), 3L);
        when(repository.findByDateBetween(DAY, DAY.plusDays(1))).thenReturn(List.of(nextDay, morning, deadline));

        assertThat(service.between(DAY, DAY.plusDays(1))).extracting(AppointmentResponse::id).containsExactly(2L, 1L, 3L);
    }

    @Test
    void anInvalidProcessNumberIsRejectedBeforeSaving() {
        AppointmentRequest request = new AppointmentRequest("Contestação", AppointmentType.DEADLINE, DAY, null, null,
                null, "0801234-00.2026.8.19.0066", null, null);

        assertThatThrownBy(() -> service.create(request)).extracting("code").isEqualTo("INVALID_PROCESS_NUMBER");
        verify(repository, never()).save(any());
    }

    private static Appointment withId(Appointment appointment, Long id) {
        ReflectionTestUtils.setField(appointment, "id", id);
        return appointment;
    }
}
