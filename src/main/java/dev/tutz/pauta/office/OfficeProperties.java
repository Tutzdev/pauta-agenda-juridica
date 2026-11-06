package dev.tutz.pauta.office;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Identificação do escritório exibida na interface. Não há login: é a mesma para quem abrir a agenda. */
@ConfigurationProperties("pauta.office")
public record OfficeProperties(String name, String lawyerName, String city) {

    public OfficeProperties {
        name = name == null || name.isBlank() ? "Escritório de Advocacia" : name.strip();
        lawyerName = lawyerName == null ? "" : lawyerName.strip();
        city = city == null ? "" : city.strip();
    }
}
