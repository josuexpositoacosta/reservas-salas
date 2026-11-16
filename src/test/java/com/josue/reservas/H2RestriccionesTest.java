package com.josue.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regresión del bug de H2 2.4: un CHECK con IN (...) queda ligado a la sesión que lo creó (la de
 * Flyway). Cuando esa sesión se cierra, todo INSERT en reservas fallaba con "Check constraint invalid
 * ... The database has been closed" y la API respondía 409 aunque la sala estuviera libre.
 *
 * En la aplicación pasa cuando el pool renueva la conexión de Flyway; aquí se reproduce sin Spring:
 * Flyway migra con su propia conexión y la cierra, y otra conexión mantiene viva la base en memoria.
 * Ver db/vendor/h2/V2__ck_estado_compatible_h2.sql.
 */
class H2RestriccionesTest {

    private final String url = "jdbc:h2:mem:regresion-" + UUID.randomUUID()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    /** Mantiene viva la base en memoria cuando Flyway cierra su conexión. */
    private Connection guardian;

    @BeforeEach
    void migrarYCerrarLaSesionDeFlyway() throws SQLException {
        guardian = DriverManager.getConnection(url, "sa", "");
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration", "classpath:db/vendor/h2")
                .load()
                .migrate();
    }

    @AfterEach
    void cerrar() throws SQLException {
        guardian.close();
    }

    @Test
    void seInsertaUnaReservaTrasCerrarseLaSesionQueCreoLasRestricciones() throws SQLException {
        try (Connection otra = DriverManager.getConnection(url, "sa", "");
                Statement sql = otra.createStatement()) {
            sql.executeUpdate("insert into salas (nombre, capacidad) values ('Sala A', 4)");

            int filas = sql.executeUpdate(insertarReserva("ACTIVA"));

            assertThat(filas).isEqualTo(1);
        }
    }

    @Test
    void elCheckDeEstadoSigueRechazandoValoresDesconocidos() throws SQLException {
        try (Connection otra = DriverManager.getConnection(url, "sa", "");
                Statement sql = otra.createStatement()) {
            sql.executeUpdate("insert into salas (nombre, capacidad) values ('Sala A', 4)");

            assertThatThrownBy(() -> sql.executeUpdate(insertarReserva("PENDIENTE")))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_reservas_estado")
                    .hasMessageNotContaining("closed");
        }
    }

    private static String insertarReserva(String estado) {
        return "insert into reservas (sala_id, responsable, inicio, fin, estado) "
                + "select id, 'Ana', timestamp '2027-01-10 10:00:00', timestamp '2027-01-10 11:00:00', '"
                + estado + "' from salas where nombre = 'Sala A'";
    }
}
