package dev.tutz.pauta.client;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.tutz.pauta.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClientApiTest extends ApiTest {

    @Test
    void createsAClientNormalizingPhoneAndEmail() throws Exception {
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "  Maria Souza ", "phone": "(24) 99812-3344", "email": "Maria@Email.com"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.phone").value("24998123344"))
                .andExpect(jsonPath("$.mobile").value(true))
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    void searchesByPartOfTheNameOrOfThePhone() throws Exception {
        createClient("{\"name\": \"Maria Souza\", \"phone\": \"24998123344\"}");
        createClient("{\"name\": \"José Ferreira\", \"phone\": \"2433421180\"}");

        mvc.perform(get("/api/clients").param("search", "souz"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Maria Souza"));
        mvc.perform(get("/api/clients").param("search", "3342-11"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("José Ferreira"));
        mvc.perform(get("/api/clients"))
                .andExpect(jsonPath("$[0].name").value("José Ferreira"));
    }

    @Test
    void wildcardsTypedInTheSearchAreTakenLiterally() throws Exception {
        createClient("{\"name\": \"Maria Souza\"}");

        mvc.perform(get("/api/clients").param("search", "%")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void countsOnlyUpcomingOpenAppointments() throws Exception {
        long client = createClient("{\"name\": \"Maria Souza\"}");
        createAppointment("""
                {"title": "Consulta", "type": "CLIENT_SERVICE", "date": "2026-11-10", "clientId": %d}
                """.formatted(client));
        createAppointment("""
                {"title": "Consulta antiga", "type": "CLIENT_SERVICE", "date": "2026-10-10", "clientId": %d}
                """.formatted(client));

        mvc.perform(get("/api/clients/{id}", client)).andExpect(jsonPath("$.upcomingAppointments").value(1));
        mvc.perform(get("/api/appointments").param("clientId", String.valueOf(client)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Consulta"));
    }

    @Test
    void aClientWithAppointmentsCannotBeDeleted() throws Exception {
        long client = createClient("{\"name\": \"Maria Souza\"}");
        createAppointment("""
                {"title": "Consulta", "type": "CLIENT_SERVICE", "date": "2026-11-10", "clientId": %d}
                """.formatted(client));

        mvc.perform(delete("/api/clients/{id}", client))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_HAS_APPOINTMENTS"));
    }

    @Test
    void aClientWithoutAppointmentsIsDeleted() throws Exception {
        long client = createClient("{\"name\": \"Maria Souza\"}");

        mvc.perform(delete("/api/clients/{id}", client)).andExpect(status().isNoContent());
        mvc.perform(get("/api/clients/{id}", client)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnInvalidPhoneOrEmail() throws Exception {
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Maria", "phone": "99812-3344"}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE"));
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Maria", "email": "maria@"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("E-mail inválido."));
    }
}
