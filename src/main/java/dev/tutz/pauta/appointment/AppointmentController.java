package dev.tutz.pauta.appointment;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping(params = {"from", "to"})
    public List<AppointmentResponse> between(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.between(from, to);
    }

    @GetMapping(params = "clientId")
    public List<AppointmentResponse> byClient(@RequestParam Long clientId) {
        return service.byClient(clientId);
    }

    @GetMapping("/deadlines")
    public List<AppointmentResponse> openDeadlines() {
        return service.openDeadlines();
    }

    @GetMapping("/conflicts")
    public List<ScheduleConflict> conflicts(@RequestParam LocalDate date, @RequestParam LocalTime startTime,
            @RequestParam(required = false) Integer durationMinutes, @RequestParam(required = false) Long excludeId) {
        return service.conflicts(date, startTime, durationMinutes, excludeId);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/appointments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/reopen")
    public AppointmentResponse reopen(@PathVariable Long id) {
        return service.reopen(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
