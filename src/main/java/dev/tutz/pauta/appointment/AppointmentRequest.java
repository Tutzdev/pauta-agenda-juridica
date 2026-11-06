package dev.tutz.pauta.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param startTime       vazio para compromisso de dia todo (o comum em prazo)
 * @param durationMinutes ignorado sem horário; com horário e sem duração, vale uma hora
 */
public record AppointmentRequest(
        @NotBlank(message = "Dê um título ao compromisso.")
        @Size(max = 160, message = "Use até 160 caracteres.")
        String title,

        @NotNull(message = "Escolha o tipo.")
        AppointmentType type,

        @NotNull(message = "Escolha a data.")
        LocalDate date,

        LocalTime startTime,

        @Min(value = 5, message = "A duração mínima é de 5 minutos.")
        @Max(value = 720, message = "A duração máxima é de 12 horas.")
        Integer durationMinutes,

        @Size(max = 160, message = "Use até 160 caracteres.")
        String location,

        @Size(max = 40, message = "Número de processo longo demais.")
        String processNumber,

        Long clientId,

        @Size(max = 4000, message = "Use até 4000 caracteres.")
        String notes) {
}
