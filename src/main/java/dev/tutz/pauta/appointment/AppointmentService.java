package dev.tutz.pauta.appointment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import dev.tutz.pauta.client.Client;
import dev.tutz.pauta.client.ClientService;
import dev.tutz.pauta.shared.BusinessRuleException;
import dev.tutz.pauta.shared.ResourceNotFoundException;
import dev.tutz.pauta.shared.Text;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    /** Uma consulta cobre no máximo três meses: o suficiente para a visão de mês com as bordas. */
    static final int MAX_RANGE_DAYS = 92;
    static final int CLIENT_HISTORY_LIMIT = 100;

    /** Por dia, os de dia todo (prazos) primeiro e depois por horário. */
    static final Comparator<Appointment> CHRONOLOGICAL = Comparator
            .comparing(Appointment::getDate)
            .thenComparing(Appointment::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Appointment::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final AppointmentRepository repository;
    private final ClientService         clientService;
    private final AppointmentMapper     mapper;
    private final Clock                 clock;

    public AppointmentService(AppointmentRepository repository, ClientService clientService,
            AppointmentMapper mapper, Clock clock) {

        this.repository    = repository;
        this.clientService = clientService;
        this.mapper        = mapper;
        this.clock         = clock;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> between(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("INVALID_RANGE", "A data inicial não pode ser depois da final.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BusinessRuleException("INVALID_RANGE",
                    "Consulte no máximo " + MAX_RANGE_DAYS + " dias por vez.");
        }
        return toResponses(repository.findByDateBetween(from, to));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> byClient(Long clientId) {
        clientService.require(clientId);
        LocalDate today = today();
        return repository.findByClientIdOrderByDateDescStartTimeDesc(clientId, Limit.of(CLIENT_HISTORY_LIMIT))
                .stream()
                .map(appointment -> mapper.toResponse(appointment, today))
                .toList();
    }

    /** Todos os prazos ainda em aberto, inclusive os vencidos, do mais antigo para o mais distante. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> openDeadlines() {
        return toResponses(repository.findByTypeAndStatus(AppointmentType.DEADLINE, AppointmentStatus.SCHEDULED));
    }

    /** Compromissos em aberto com data passada, de qualquer tipo: ninguém deu baixa neles. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> overdue() {
        return toResponses(repository.findByStatusAndDateBefore(AppointmentStatus.SCHEDULED, today()));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id) {
        return mapper.toResponse(require(id), today());
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        Appointment appointment = new Appointment(detailsOf(request), clientOf(request), now());
        return mapper.toResponse(repository.save(appointment), today());
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = require(id);
        appointment.update(detailsOf(request), clientOf(request), now());
        return mapper.toResponse(appointment, today());
    }

    @Transactional
    public AppointmentResponse complete(Long id) {
        Appointment appointment = require(id);
        appointment.complete(now());
        return mapper.toResponse(appointment, today());
    }

    @Transactional
    public AppointmentResponse cancel(Long id) {
        Appointment appointment = require(id);
        appointment.cancel(now());
        return mapper.toResponse(appointment, today());
    }

    @Transactional
    public AppointmentResponse reopen(Long id) {
        Appointment appointment = require(id);
        appointment.reopen(now());
        return mapper.toResponse(appointment, today());
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    /**
     * Compromissos em aberto no mesmo dia cujo horário se sobrepõe ao informado.
     *
     * @param excludeId o próprio compromisso, quando a checagem é feita durante a edição
     */
    @Transactional(readOnly = true)
    public List<ScheduleConflict> conflicts(LocalDate date, LocalTime startTime, Integer durationMinutes,
            Long excludeId) {

        int duration = durationMinutes == null ? AppointmentDetails.DEFAULT_DURATION_MINUTES : durationMinutes;
        if (duration < 5 || duration > 720) {
            throw new BusinessRuleException("INVALID_DURATION", "A duração precisa estar entre 5 minutos e 12 horas.");
        }
        TimeSlot wanted = TimeSlot.of(startTime, duration);

        return repository.findByDateAndStatusAndStartTimeNotNull(date, AppointmentStatus.SCHEDULED).stream()
                .filter(other -> !Objects.equals(other.getId(), excludeId))
                .filter(other -> other.slot().overlaps(wanted))
                .sorted(CHRONOLOGICAL)
                .map(mapper::toConflict)
                .toList();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        LocalDate today = today();
        return appointments.stream()
                .sorted(CHRONOLOGICAL)
                .map(appointment -> mapper.toResponse(appointment, today))
                .toList();
    }

    private Appointment require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compromisso não encontrado."));
    }

    private Client clientOf(AppointmentRequest request) {
        return request.clientId() == null ? null : clientService.require(request.clientId());
    }

    private static AppointmentDetails detailsOf(AppointmentRequest request) {
        String processNumber = Text.trimToNull(request.processNumber());
        return new AppointmentDetails(
                request.title(),
                request.type(),
                request.date(),
                request.startTime(),
                request.durationMinutes(),
                request.location(),
                processNumber == null ? null : ProcessNumber.parse(processNumber),
                request.notes());
    }

    private Instant now() {
        return clock.instant();
    }
}
