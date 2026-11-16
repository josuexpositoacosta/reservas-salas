package com.josue.reservas.reserva;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CrearReservaRequest(
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser un id válido")
        Long salaId,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        String responsable,

        @NotNull(message = "es obligatorio")
        @Future(message = "debe ser una fecha futura")
        LocalDateTime inicio,

        @NotNull(message = "es obligatorio")
        LocalDateTime fin) {
}
