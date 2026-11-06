package dev.tutz.pauta.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import dev.tutz.pauta.client.Client;
import dev.tutz.pauta.shared.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment {

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(length = 160)
    private String location;

    @Column(name = "process_number", length = 25)
    private String processNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(length = 4000)
    private String notes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
    }

    Appointment(AppointmentDetails details, Client client, Instant now) {
        this.status    = AppointmentStatus.SCHEDULED;
        this.createdAt = now;
        update(details, client, now);
    }

    void update(AppointmentDetails details, Client client, Instant now) {
        this.title           = details.title();
        this.type            = details.type();
        this.date            = details.date();
        this.startTime       = details.startTime();
        this.durationMinutes = details.durationMinutes();
        this.location        = details.location();
        this.processNumber   = details.processNumber() == null ? null : details.processNumber().value();
        this.notes           = details.notes();
        this.client          = client;
        this.updatedAt       = now;
    }

    void complete(Instant now) {
        transitionTo(AppointmentStatus.DONE, now);
        completedAt = now;
    }

    void cancel(Instant now) {
        transitionTo(AppointmentStatus.CANCELED, now);
    }

    /** Desfaz "feito" ou "cancelado": volta para a agenda como em aberto. */
    void reopen(Instant now) {
        transitionTo(AppointmentStatus.SCHEDULED, now);
        completedAt = null;
    }

    boolean isOpen() {
        return status == AppointmentStatus.SCHEDULED;
    }

    /** Intervalo ocupado no dia; {@code null} quando o compromisso não tem horário (dia todo). */
    TimeSlot slot() {
        return startTime == null ? null : TimeSlot.of(startTime, durationMinutes);
    }

    private void transitionTo(AppointmentStatus next, Instant now) {
        if (!ALLOWED_TRANSITIONS.get(status).contains(next)) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION", switch (next) {
                case DONE -> "Só um compromisso em aberto pode ser marcado como feito.";
                case CANCELED -> "Só um compromisso em aberto pode ser cancelado.";
                case SCHEDULED -> "Este compromisso já está em aberto.";
            });
        }
        status = next;
        updatedAt = now;
    }

    private static Map<AppointmentStatus, Set<AppointmentStatus>> allowedTransitions() {
        Map<AppointmentStatus, Set<AppointmentStatus>> transitions = new EnumMap<>(AppointmentStatus.class);

        transitions.put(AppointmentStatus.SCHEDULED, EnumSet.of(AppointmentStatus.DONE, AppointmentStatus.CANCELED));
        transitions.put(AppointmentStatus.DONE, EnumSet.of(AppointmentStatus.SCHEDULED));
        transitions.put(AppointmentStatus.CANCELED, EnumSet.of(AppointmentStatus.SCHEDULED));

        return Map.copyOf(transitions);
    }

    public Long getId()                  { return id; }
    public String getTitle()             { return title; }
    public AppointmentType getType()     { return type; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDate getDate()           { return date; }
    public LocalTime getStartTime()      { return startTime; }
    public Integer getDurationMinutes()  { return durationMinutes; }
    public String getLocation()          { return location; }
    public String getProcessNumber()     { return processNumber; }
    public Client getClient()            { return client; }
    public String getNotes()             { return notes; }
    public Instant getCompletedAt()      { return completedAt; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }
}
