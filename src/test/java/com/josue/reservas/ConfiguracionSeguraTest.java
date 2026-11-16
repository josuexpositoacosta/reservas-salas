package com.josue.reservas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;

/**
 * Fuera del perfil "dev" no hay credenciales por defecto: si no se configuran, la aplicación
 * debe negarse a arrancar en lugar de hacerlo con un secreto JWT conocido.
 */
class ConfiguracionSeguraTest {

    @Test
    void fueraDeDevNoArrancaSinCredenciales() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(ReservasSalasApplication.class)
                        .profiles("prod")
                        .properties("server.port=0")
                        .run()
                        .close())
                .rootCause()
                .isInstanceOf(BindValidationException.class)
                .hasMessageContaining("app.seguridad");
    }
}
