package com.josue.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.josue.reservas.comun.ConflictoException;
import com.josue.reservas.reserva.CrearReservaRequest;
import com.josue.reservas.reserva.ReservaService;
import com.josue.reservas.sala.CrearSalaRequest;
import com.josue.reservas.sala.SalaService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Lo mismo que en producción: PostgreSQL real (en un contenedor desechable) y las migraciones
 * específicas de PostgreSQL, que con H2 no se ejecutan. Requiere Docker.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SalaService salas;

    @Autowired
    ReservaService reservas;

    @Test
    void flywayAplicaTambienLaMigracionEspecificaDePostgres() {
        List<String> versiones = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(versiones).containsExactly("1", "2");
    }

    /** La red de seguridad: aunque alguien escriba sin pasar por la API, la BD rechaza el solapamiento. */
    @Test
    void laRestriccionDeExclusionRechazaSolapamientosEscritosDirectamenteEnLaBd() {
        long sala = crearSala();
        insertar(sala, "2030-10-01T10:00", "2030-10-01T11:00", "ACTIVA");

        assertThatThrownBy(() -> insertar(sala, "2030-10-01T10:30", "2030-10-01T11:30", "ACTIVA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_reservas_sin_solapamiento");

        // Lo que sí debe permitir: consecutivas, canceladas que solapan y la misma hora en otra sala
        insertar(sala, "2030-10-01T11:00", "2030-10-01T12:00", "ACTIVA");
        insertar(sala, "2030-10-01T10:15", "2030-10-01T10:45", "CANCELADA");
        insertar(crearSala(), "2030-10-01T10:00", "2030-10-01T11:00", "ACTIVA");
    }

    @Test
    void enPostgresSoloUnaDeVariasReservasSimultaneasTieneExito() throws Exception {
        long sala = crearSala();
        LocalDateTime inicio = LocalDateTime.of(2030, 10, 2, 10, 0);
        int hilos = 10;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        int exitos = 0;
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                String responsable = "Persona " + i;
                resultados.add(pool.submit(() -> {
                    salida.await();
                    try {
                        reservas.crear(new CrearReservaRequest(sala, responsable, inicio, inicio.plusHours(1)));
                        return true;
                    } catch (ConflictoException e) {
                        return false;
                    }
                }));
            }
            salida.countDown();
            for (Future<Boolean> r : resultados) {
                exitos += r.get() ? 1 : 0;
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(exitos).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from reservas where sala_id = ?", Integer.class, sala))
                .isEqualTo(1);
    }

    private long crearSala() {
        return salas.crear(new CrearSalaRequest("PG " + UUID.randomUUID().toString().substring(0, 8), 6)).id();
    }

    private void insertar(long sala, String inicio, String fin, String estado) {
        jdbc.update("insert into reservas (sala_id, responsable, inicio, fin, estado) values (?, 'SQL directo', ?, ?, ?)",
                sala, LocalDateTime.parse(inicio), LocalDateTime.parse(fin), estado);
    }
}
