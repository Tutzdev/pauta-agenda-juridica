package dev.tutz.pauta.dashboard;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import dev.tutz.pauta.support.ApiTest;
import org.junit.jupiter.api.Test;

class DashboardApiTest extends ApiTest {

    @Test
    void summarizesTodayWhatNeedsAttentionAndTheNextDeadlines() throws Exception {
        createClient("{\"name\": \"Maria Souza\"}");
        createAppointment("{\"title\": \"Réplica\", \"type\": \"DEADLINE\", \"date\": \"2026-11-04\"}");
        createAppointment("{\"title\": \"Contestação\", \"type\": \"DEADLINE\", \"date\": \"2026-11-06\"}");
        createAppointment("""
                {"title": "Consulta", "type": "CLIENT_SERVICE", "date": "2026-11-06", "startTime": "14:00"}
                """);
        long done = createAppointment("""
                {"title": "Ligação", "type": "MEETING", "date": "2026-11-06", "startTime": "08:00"}
                """);
        mvc.perform(post("/api/appointments/{id}/complete", done));
        createAppointment("{\"title\": \"Recurso\", \"type\": \"DEADLINE\", \"date\": \"2026-11-10\"}");
        createAppointment("{\"title\": \"Alegações\", \"type\": \"DEADLINE\", \"date\": \"2026-11-30\"}");

        mvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.today").value("2026-11-06"))
                .andExpect(jsonPath("$.agendaToday", hasSize(3)))
                .andExpect(jsonPath("$.agendaToday[0].title").value("Contestação"))
                .andExpect(jsonPath("$.attention[*].title").value(contains("Réplica", "Contestação")))
                .andExpect(jsonPath("$.upcomingDeadlines[*].title").value(contains("Recurso")))
                .andExpect(jsonPath("$.nextDays", hasSize(7)))
                .andExpect(jsonPath("$.nextDays[0].open").value(2))
                .andExpect(jsonPath("$.nextDays[4].date").value("2026-11-10"))
                .andExpect(jsonPath("$.nextDays[4].open").value(1))
                .andExpect(jsonPath("$.totals.today").value(2))
                .andExpect(jsonPath("$.totals.nextSevenDays").value(3))
                .andExpect(jsonPath("$.totals.overdue").value(1))
                .andExpect(jsonPath("$.totals.upcomingDeadlines").value(1))
                .andExpect(jsonPath("$.totals.clients").value(1));
    }

    @Test
    void officeInfoUsesTheServerDate() throws Exception {
        mvc.perform(get("/api/office"))
                .andExpect(jsonPath("$.lawyerName").value("Dra. Teste"))
                .andExpect(jsonPath("$.name").value("Escritório de Advocacia"))
                .andExpect(jsonPath("$.today").value("2026-11-06"));
    }

    @Test
    void calculatesADeadlineInBusinessDays() throws Exception {
        mvc.perform(get("/api/deadlines/calculate").param("startDate", "2026-11-06").param("businessDays", "15"))
                .andExpect(jsonPath("$.dueDate").value("2026-11-30"))
                .andExpect(jsonPath("$.skipped[0].reason").value("Dia da Consciência Negra"));
    }
}
