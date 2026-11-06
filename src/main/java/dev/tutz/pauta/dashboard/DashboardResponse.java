package dev.tutz.pauta.dashboard;

import java.time.LocalDate;
import java.util.List;

import dev.tutz.pauta.appointment.AppointmentResponse;

/**
 * @param agendaToday       tudo o que é de hoje, inclusive o que já foi feito
 * @param attention         em aberto e atrasado, mais os prazos que vencem hoje
 * @param upcomingDeadlines prazos dos próximos dias (sem os de hoje)
 * @param nextDays          compromissos em aberto em cada um dos próximos sete dias, a partir de hoje
 */
public record DashboardResponse(LocalDate today, List<AppointmentResponse> agendaToday,
                                List<AppointmentResponse> attention, List<AppointmentResponse> upcomingDeadlines,
                                List<DayLoad> nextDays, Totals totals) {

    public record DayLoad(LocalDate date, long open) {
    }

    public record Totals(long today, long nextSevenDays, long overdue, long upcomingDeadlines, long clients) {
    }
}
