package dev.tutz.pauta.appointment;

/** Projeção da contagem por cliente; pública porque o Hibernate a instancia pelo {@code select new}. */
public record ClientAppointmentCount(Long clientId, long count) {
}
