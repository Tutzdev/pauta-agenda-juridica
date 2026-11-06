package dev.tutz.pauta.shared;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    /** O "hoje" do escritório é sempre o de Brasília, independente do fuso da máquina. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock businessClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
