package dev.tutz.pauta.client;

/**
 * @param upcomingAppointments compromissos em aberto de hoje em diante, para a lista mostrar quem tem agenda marcada
 */
public record ClientResponse(Long id, String name, String phone, boolean mobile, String email, String notes,
                             long upcomingAppointments) {

    static ClientResponse of(Client client, long upcomingAppointments) {
        boolean mobile = client.getPhone() != null && new PhoneNumber(client.getPhone()).isMobile();
        return new ClientResponse(client.getId(), client.getName(), client.getPhone(), mobile, client.getEmail(),
                client.getNotes(), upcomingAppointments);
    }
}
