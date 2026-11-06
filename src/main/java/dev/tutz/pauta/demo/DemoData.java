package dev.tutz.pauta.demo;

import java.time.LocalDate;
import java.time.LocalTime;

import dev.tutz.pauta.appointment.AppointmentRequest;
import dev.tutz.pauta.appointment.AppointmentResponse;
import dev.tutz.pauta.appointment.AppointmentService;
import dev.tutz.pauta.appointment.AppointmentType;
import dev.tutz.pauta.client.ClientRequest;
import dev.tutz.pauta.client.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Preenche um banco vazio com clientes e compromissos fictícios em torno de hoje, para
 * apresentar a agenda. Só roda no perfil {@code demo}, que usa um arquivo de banco próprio.
 */
@Component
@Profile("demo")
class DemoData implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoData.class);

    private static final String FORUM = "Fórum de Volta Redonda";
    private static final String OFFICE = "Escritório";

    private final ClientService      clients;
    private final AppointmentService appointments;

    DemoData(ClientService clients, AppointmentService appointments) {
        this.clients      = clients;
        this.appointments = appointments;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (clients.count() > 0) {
            return;
        }
        LocalDate today = appointments.today();

        Long maria = client("Maria Aparecida Souza", "(24) 99812-3344", "maria.souza@email.com",
                "Prefere contato por WhatsApp à tarde.");
        Long jose = client("José Carlos Ferreira", "(24) 99901-2277", null, null);
        Long acoForte = client("Comercial Aço Forte Ltda.", "(24) 3342-1180", "juridico@acoforte.com.br",
                "Falar com o Sr. Paulo, do financeiro.");
        Long ana = client("Ana Beatriz Lima", "(24) 99755-6610", "anabeatriz.lima@email.com", null);
        Long roberto = client("Roberto Nunes", "(24) 98833-4521", null, "Indicação da Maria Souza.");
        Long luciana = client("Luciana Prado", "(24) 99210-7788", "luciana.prado@email.com", null);

        add("Réplica à contestação", AppointmentType.DEADLINE, weekday(today.minusDays(2)), null, null, null,
                "0801234-67.2026.8.19.0066", maria, null);
        AppointmentResponse consultation = add("Retorno sobre documentos", AppointmentType.CLIENT_SERVICE,
                weekday(today.minusDays(1)), LocalTime.of(15, 0), 30, OFFICE, null, roberto, null);
        appointments.complete(consultation.id());

        add("Audiência de conciliação", AppointmentType.HEARING, today, LocalTime.of(9, 30), 90,
                FORUM + " · 2ª Vara Cível", "0012345-81.2026.8.19.0066", jose,
                "Levar procuração e comprovante de residência atualizado.");
        add("Contestação", AppointmentType.DEADLINE, today, null, null, null, "0805678-46.2026.8.19.0066",
                acoForte, "Prazo de 15 dias úteis, intimação pelo DJE.");
        add("Primeira consulta", AppointmentType.CLIENT_SERVICE, today, LocalTime.of(14, 0), 60, OFFICE, null,
                ana, "Questão de pensão alimentícia.");
        add("Ligação com o perito", AppointmentType.MEETING, today, LocalTime.of(14, 30), 30, "Telefone", null,
                null, null);

        add("Cópia dos autos no cartório", AppointmentType.DILIGENCE, weekday(today.plusDays(1)),
                LocalTime.of(10, 0), 60, FORUM + " · Cartório da 1ª Vara de Família", null, maria, null);
        add("Recurso inominado", AppointmentType.DEADLINE, weekday(today.plusDays(2)), null, null, null,
                "0100321-29.2026.8.19.0066", roberto, null);
        add("Audiência de instrução", AppointmentType.HEARING, weekday(today.plusDays(3)), LocalTime.of(13, 0), 120,
                "Vara do Trabalho de Volta Redonda", "0100321-64.2025.5.01.0341", luciana,
                "Duas testemunhas confirmadas.");
        add("Manifestação sobre o laudo", AppointmentType.DEADLINE, weekday(today.plusDays(4)), null, null, null,
                "0012345-81.2026.8.19.0066", jose, null);
        add("Retorno da consulta", AppointmentType.CLIENT_SERVICE, weekday(today.plusDays(6)), LocalTime.of(16, 0),
                45, OFFICE, null, ana, null);
        add("Audiência una (Juizado Especial)", AppointmentType.HEARING, weekday(today.plusDays(9)),
                LocalTime.of(11, 0), 60, FORUM + " · Juizado Especial Cível", "0002468-20.2026.8.19.0066",
                maria, null);
        add("Alegações finais", AppointmentType.DEADLINE, weekday(today.plusDays(12)), null, null, null,
                "0805678-46.2026.8.19.0066", acoForte, null);
        add("Reunião sobre acordo", AppointmentType.MEETING, weekday(today.plusDays(15)), LocalTime.of(10, 0), 60,
                "Videochamada", null, acoForte, null);
        add("Pagar anuidade da OAB", AppointmentType.OTHER, weekday(today.plusDays(20)), null, null, null, null,
                null, null);

        LOGGER.info("Perfil demo: agenda preenchida com dados de exemplo.");
    }

    private Long client(String name, String phone, String email, String notes) {
        return clients.create(new ClientRequest(name, phone, email, notes)).id();
    }

    private AppointmentResponse add(String title, AppointmentType type, LocalDate date, LocalTime time,
            Integer duration, String location, String processNumber, Long clientId, String notes) {
        return appointments.create(new AppointmentRequest(title, type, date, time, duration, location,
                processNumber, clientId, notes));
    }

    /** Leva sábado e domingo para a segunda seguinte, como a agenda de verdade. */
    private static LocalDate weekday(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.plusDays(2);
            case SUNDAY -> date.plusDays(1);
            default -> date;
        };
    }
}
