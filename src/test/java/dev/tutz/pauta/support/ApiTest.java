package dev.tutz.pauta.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import dev.tutz.pauta.shared.ApplicationConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sobe a aplicação inteira com as migrações Flyway num H2 em memória. O relógio é fixo em
 * sexta-feira, 06/11/2026, 10h de Brasília; cada teste roda numa transação desfeita no fim.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(ApiTest.FixedClock.class)
public abstract class ApiTest {

    protected static final Instant NOW = Instant.parse("2026-11-06T13:00:00Z");

    @Autowired
    protected MockMvc mvc;

    protected long createClient(String json) throws Exception {
        return idOf(mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    protected long createAppointment(String json) throws Exception {
        return idOf(mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private static long idOf(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @TestConfiguration
    static class FixedClock {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ApplicationConfiguration.BUSINESS_ZONE);
        }
    }
}
