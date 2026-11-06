package dev.tutz.pauta.appointment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.tutz.pauta.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AppointmentApiTest extends ApiTest {

    private static final String HEARING = """
            {"title": "Audiência de conciliação", "type": "HEARING", "date": "2026-11-10",
             "startTime": "14:00", "durationMinutes": 90, "location": "Fórum de Volta Redonda",
             "processNumber": "08012346720268190066"}
            """;

    @Test
    void createsAnAppointmentAndFormatsTheProcessNumber() throws Exception {
        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(HEARING))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/appointments/")))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.startTime").value("14:00"))
                .andExpect(jsonPath("$.endTime").value("15:30"))
                .andExpect(jsonPath("$.processNumber").value("0801234-67.2026.8.19.0066"))
                .andExpect(jsonPath("$.alert").value("NONE"));
    }

    @Test
    void aDeadlineDueTodayIsFlaggedAndHasNoTime() throws Exception {
        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Contestação", "type": "DEADLINE", "date": "2026-11-06", "durationMinutes": 30}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alert").value("TODAY"))
                .andExpect(jsonPath("$.startTime").doesNotExist())
                .andExpect(jsonPath("$.durationMinutes").doesNotExist());
    }

    @Test
    void missingFieldsComeBackOneMessagePerField() throws Exception {
        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FIELDS"))
                .andExpect(jsonPath("$.fieldErrors.title").value("Dê um título ao compromisso."))
                .andExpect(jsonPath("$.fieldErrors.type").value("Escolha o tipo."))
                .andExpect(jsonPath("$.fieldErrors.date").value("Escolha a data."));
    }

    @Test
    void aMistypedProcessNumberIsRejectedWithItsOwnCode() throws Exception {
        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Recurso", "type": "DEADLINE", "date": "2026-11-12",
                         "processNumber": "0801234-68.2026.8.19.0066"}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_PROCESS_NUMBER"));
    }

    @Test
    void markingDoneTwiceIsAnInvalidTransition() throws Exception {
        long id = createAppointment(HEARING);

        mvc.perform(post("/api/appointments/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").exists());
        mvc.perform(post("/api/appointments/{id}/complete", id))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        mvc.perform(post("/api/appointments/{id}/reopen", id))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void warnsAboutOverlappingAppointmentsButNotAboutTheOneBeingEdited() throws Exception {
        long id = createAppointment(HEARING);

        mvc.perform(get("/api/appointments/conflicts")
                        .param("date", "2026-11-10").param("startTime", "15:00").param("durationMinutes", "60"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Audiência de conciliação"))
                .andExpect(jsonPath("$[0].endTime").value("15:30"));
        mvc.perform(get("/api/appointments/conflicts")
                        .param("date", "2026-11-10").param("startTime", "15:00").param("excludeId", String.valueOf(id)))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/appointments/conflicts").param("date", "2026-11-10").param("startTime", "15:30"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsARangeAndTheOpenDeadlines() throws Exception {
        createAppointment(HEARING);
        createAppointment("""
                {"title": "Réplica", "type": "DEADLINE", "date": "2026-11-04"}
                """);
        createAppointment("""
                {"title": "Alegações finais", "type": "DEADLINE", "date": "2026-12-15"}
                """);

        mvc.perform(get("/api/appointments").param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Réplica"))
                .andExpect(jsonPath("$[0].alert").value("OVERDUE"));
        mvc.perform(get("/api/appointments/deadlines"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].title").value("Alegações finais"));
    }

    @Test
    void editsAndDeletes() throws Exception {
        long id = createAppointment(HEARING);

        mvc.perform(put("/api/appointments/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Audiência remarcada", "type": "HEARING", "date": "2026-11-11", "startTime": "09:00"}
                        """))
                .andExpect(jsonPath("$.date").value("2026-11-11"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.processNumber").doesNotExist());

        mvc.perform(delete("/api/appointments/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Compromisso não encontrado."));
    }

    @Test
    void linkingAClientThatDoesNotExistIsNotFound() throws Exception {
        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Consulta", "type": "CLIENT_SERVICE", "date": "2026-11-10", "clientId": 999}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cliente não encontrado."));
    }

    @Test
    void responsesCarryBrowserProtectionHeaders() throws Exception {
        mvc.perform(get("/api/appointments/deadlines"))
                .andExpect(header().string("Content-Security-Policy", startsWith("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
