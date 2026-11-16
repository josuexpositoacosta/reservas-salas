package com.josue.reservas.seguridad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuración de app.seguridad.*; si falta algo o el secreto es corto, la aplicación no arranca. */
@Validated
@ConfigurationProperties("app.seguridad")
public record PropiedadesSeguridad(
        @NotBlank String usuario,
        @NotBlank String clave,
        @NotBlank @Size(min = 32, message = "debe tener al menos 32 caracteres (HS256)") String jwtSecreto,
        @NotNull Duration jwtDuracion) {
}
