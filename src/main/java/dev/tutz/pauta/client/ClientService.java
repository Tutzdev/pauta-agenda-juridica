package dev.tutz.pauta.client;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import dev.tutz.pauta.shared.ConflictException;
import dev.tutz.pauta.shared.ResourceNotFoundException;
import dev.tutz.pauta.shared.Text;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    static final int MAX_RESULTS = 200;

    private final ClientRepository clientRepository;
    private final ClientSchedule   schedule;
    private final Clock            clock;

    public ClientService(ClientRepository clientRepository, ClientSchedule schedule, Clock clock) {
        this.clientRepository = clientRepository;
        this.schedule         = schedule;
        this.clock            = clock;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> list(String search) {
        String query = Text.trimToNull(search);
        List<Client> clients = query == null
                ? clientRepository.findAllByOrderByNameAsc(Limit.of(MAX_RESULTS))
                : searchBy(query);

        Map<Long, Long> upcoming = schedule.upcomingCountByClient(clients.stream().map(Client::getId).toList());
        return clients.stream()
                .map(client -> ClientResponse.of(client, upcoming.getOrDefault(client.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse get(Long id) {
        Client client = require(id);
        return ClientResponse.of(client, schedule.upcomingCountByClient(List.of(id)).getOrDefault(id, 0L));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = new Client(request.name(), phoneOf(request), request.email(), request.notes(), now());
        return ClientResponse.of(clientRepository.save(client), 0);
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = require(id);
        client.update(request.name(), phoneOf(request), request.email(), request.notes(), now());
        return get(id);
    }

    /** Cliente com histórico na agenda não é apagado, para nenhum compromisso perder a referência. */
    @Transactional
    public void delete(Long id) {
        Client client = require(id);
        if (schedule.hasAppointments(id)) {
            throw new ConflictException("CLIENT_HAS_APPOINTMENTS",
                    "Este cliente tem compromissos na agenda. Remova ou troque o cliente deles antes de excluí-lo.");
        }
        clientRepository.delete(client);
    }

    @Transactional(readOnly = true)
    public long count() {
        return clientRepository.count();
    }

    /** Para os outros módulos vincularem um cliente existente. */
    @Transactional(readOnly = true)
    public Client require(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
    }

    private List<Client> searchBy(String query) {
        String term = "%" + escapeLike(query.toLowerCase()) + "%";
        String digits = query.replaceAll("\\D", "");
        return clientRepository.search(term, digits, "%" + digits + "%", Limit.of(MAX_RESULTS));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static PhoneNumber phoneOf(ClientRequest request) {
        String phone = Text.trimToNull(request.phone());
        return phone == null ? null : PhoneNumber.parse(phone);
    }

    private Instant now() {
        return clock.instant();
    }
}
