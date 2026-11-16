package com.josue.reservas.comun;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI generada desde el código: /v3/api-docs (JSON) y /swagger-ui.html (navegador).
 * El esquema "bearerAuth" activa el botón «Authorize» de Swagger UI para probar los endpoints protegidos.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Reserva de salas",
        version = "1.0",
        description = """
                API para reservar salas de reuniones sin solapamientos.

                **Para probar los endpoints protegidos:**
                1. Ejecuta `POST /api/auth/login` (en el perfil dev: `admin` / `admin123`).
                2. Copia el `token` de la respuesta.
                3. Pulsa **Authorize** y pégalo.

                Las consultas (GET) no necesitan token."""))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
