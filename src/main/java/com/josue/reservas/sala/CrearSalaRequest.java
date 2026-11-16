package com.josue.reservas.sala;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearSalaRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "es obligatoria")
        @Positive(message = "debe ser mayor que 0")
        @Max(value = 1000, message = "no puede superar 1000 personas")
        Integer capacidad) {
}
