package dev.tutz.pauta.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank(message = "Informe o nome do cliente.")
        @Size(max = 120, message = "Use até 120 caracteres.")
        String name,

        @Size(max = 30, message = "Telefone longo demais.")
        String phone,

        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail longo demais.")
        String email,

        @Size(max = 2000, message = "Use até 2000 caracteres.")
        String notes) {
}
