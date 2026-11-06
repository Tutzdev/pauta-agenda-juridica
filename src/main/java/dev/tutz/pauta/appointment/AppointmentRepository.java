package dev.tutz.pauta.appointment;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = "client")
    List<Appointment> findByDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "client")
    List<Appointment> findByTypeAndStatus(AppointmentType type, AppointmentStatus status);

    @EntityGraph(attributePaths = "client")
    List<Appointment> findByStatusAndDateBefore(AppointmentStatus status, LocalDate date);

    @EntityGraph(attributePaths = "client")
    List<Appointment> findByClientIdOrderByDateDescStartTimeDesc(Long clientId, Limit limit);

    List<Appointment> findByDateAndStatusAndStartTimeNotNull(LocalDate date, AppointmentStatus status);

    boolean existsByClientId(Long clientId);

    @Query("""
            select new dev.tutz.pauta.appointment.ClientAppointmentCount(a.client.id, count(a))
            from Appointment a
            where a.client.id in :clientIds and a.status = :status and a.date >= :from
            group by a.client.id
            """)
    List<ClientAppointmentCount> countByClientSince(@Param("clientIds") Collection<Long> clientIds,
            @Param("status") AppointmentStatus status, @Param("from") LocalDate from);
}
