package dev.tutz.pauta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PautaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PautaApplication.class, args);
    }
}
