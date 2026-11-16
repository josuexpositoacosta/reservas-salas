package com.josue.reservas.seguridad;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "es obligatorio") String usuario,
        @NotBlank(message = "es obligatoria") String clave) {
}
