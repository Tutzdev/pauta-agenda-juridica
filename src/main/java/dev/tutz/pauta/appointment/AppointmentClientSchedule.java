package dev.tutz.pauta.appointment;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import dev.tutz.pauta.client.ClientSchedule;
import org.springframework.stereotype.Component;

@Component
class AppointmentClientSchedule implements ClientSchedule {

    private final AppointmentRepository repository;
    private final Clock                 clock;

    AppointmentClientSchedule(AppointmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock      = clock;
    }

    @Override
    public Map<Long, Long> upcomingCountByClient(Collection<Long> clientIds) {
        if (clientIds.isEmpty()) {
            return Map.of();
        }
        return repository.countByClientSince(clientIds, AppointmentStatus.SCHEDULED, LocalDate.now(clock)).stream()
                .collect(Collectors.toUnmodifiableMap(ClientAppointmentCount::clientId, ClientAppointmentCount::count));
    }

    @Override
    public boolean hasAppointments(Long clientId) {
        return repository.existsByClientId(clientId);
    }
}
