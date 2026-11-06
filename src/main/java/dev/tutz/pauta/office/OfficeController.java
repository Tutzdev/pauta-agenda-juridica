package dev.tutz.pauta.office;

import java.time.Clock;
import java.time.LocalDate;

import dev.tutz.pauta.appointment.AlertProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/office")
public class OfficeController {

    private final OfficeProperties office;
    private final AlertProperties  alerts;
    private final Clock            clock;

    public OfficeController(OfficeProperties office, AlertProperties alerts, Clock clock) {
        this.office = office;
        this.alerts = alerts;
        this.clock  = clock;
    }

    /** O "hoje" vem do servidor, no fuso do escritório, para a tela não depender do relógio do navegador. */
    @GetMapping
    public OfficeResponse get() {
        return new OfficeResponse(office.name(), office.lawyerName(), office.city(), alerts.upcomingDays(),
                LocalDate.now(clock));
    }

    public record OfficeResponse(String name, String lawyerName, String city, int upcomingDays, LocalDate today) {
    }
}
