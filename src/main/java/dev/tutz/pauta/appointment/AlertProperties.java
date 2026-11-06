package dev.tutz.pauta.appointment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param upcomingDays prazos que vencem em até N dias corridos aparecem como "próximos" */
@ConfigurationProperties("pauta.alerts")
public record AlertProperties(int upcomingDays) {

    public AlertProperties {
        if (upcomingDays < 0 || upcomingDays > 60) {
            throw new IllegalArgumentException("pauta.alerts.upcoming-days precisa estar entre 0 e 60.");
        }
    }
}
