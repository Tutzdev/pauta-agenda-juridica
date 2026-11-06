package dev.tutz.pauta.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.tutz.pauta.appointment.Alert;
import dev.tutz.pauta.appointment.AppointmentResponse;
import dev.tutz.pauta.appointment.AppointmentService;
import dev.tutz.pauta.appointment.AppointmentStatus;
import dev.tutz.pauta.appointment.AppointmentType;
import dev.tutz.pauta.client.ClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    static final int NEXT_DAYS = 7;

    private final AppointmentService appointmentService;
    private final ClientService      clientService;

    public DashboardService(AppointmentService appointmentService, ClientService clientService) {
        this.appointmentService = appointmentService;
        this.clientService      = clientService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate today = appointmentService.today();
        List<AppointmentResponse> week = appointmentService.between(today, today.plusDays(NEXT_DAYS - 1));
        List<AppointmentResponse> agendaToday = week.stream().filter(item -> item.date().isEqual(today)).toList();
        List<AppointmentResponse> overdue = appointmentService.overdue();

        List<AppointmentResponse> deadlinesToday = agendaToday.stream()
                .filter(item -> item.type() == AppointmentType.DEADLINE && item.alert() == Alert.TODAY)
                .toList();
        List<AppointmentResponse> attention = Stream.concat(overdue.stream(), deadlinesToday.stream()).toList();

        List<AppointmentResponse> upcomingDeadlines = appointmentService.openDeadlines().stream()
                .filter(item -> item.alert() == Alert.UPCOMING)
                .toList();

        Map<LocalDate, Long> openByDay = week.stream()
                .filter(DashboardService::isOpen)
                .collect(Collectors.groupingBy(AppointmentResponse::date, Collectors.counting()));
        List<DashboardResponse.DayLoad> nextDays = Stream.iterate(today, day -> day.plusDays(1))
                .limit(NEXT_DAYS)
                .map(day -> new DashboardResponse.DayLoad(day, openByDay.getOrDefault(day, 0L)))
                .toList();

        DashboardResponse.Totals totals = new DashboardResponse.Totals(
                agendaToday.stream().filter(DashboardService::isOpen).count(),
                openByDay.values().stream().mapToLong(Long::longValue).sum(),
                overdue.size(),
                upcomingDeadlines.size(),
                clientService.count());

        return new DashboardResponse(today, agendaToday, attention, upcomingDeadlines, nextDays, totals);
    }

    private static boolean isOpen(AppointmentResponse appointment) {
        return appointment.status() == AppointmentStatus.SCHEDULED;
    }
}
