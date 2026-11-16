package com.josue.reservas.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import com.josue.reservas.comun.ConflictoException;
import com.josue.reservas.sala.CrearSalaRequest;
import com.josue.reservas.sala.SalaService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sin el bloqueo de la sala, varias peticiones simultáneas pasarían a la vez la comprobación
 * "¿está libre?" y se guardarían reservas solapadas. Aquí 10 hilos salen a la vez a por el mismo hueco.
 */
@SpringBootTest
class ReservasConcurrentesTest {

    private static final int HILOS = 10;

    @Autowired
    ReservaService reservas;

    @Autowired
    SalaService salas;

    @Autowired
    ReservaRepository repositorio;

    @Test
    void soloUnaDeVariasReservasSimultaneasAlMismoHuecoTieneExito() throws Exception {
        Long salaId = salas.crear(new CrearSalaRequest("Concurrencia", 10)).id();
        LocalDateTime inicio = LocalDateTime.of(2030, 9, 1, 10, 0);
        CountDownLatch salida = new CountDownLatch(1);

        int exitos = 0;
        int conflictos = 0;
        ExecutorService pool = Executors.newFixedThreadPool(HILOS); // Java 17: no es AutoCloseable
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < HILOS; i++) {
                String responsable = "Persona " + i;
                Callable<Boolean> intento = () -> {
                    salida.await(); // todos arrancan a la vez
                    try {
                        reservas.crear(new CrearReservaRequest(salaId, responsable, inicio, inicio.plusHours(1)));
                        return true;
                    } catch (ConflictoException e) {
                        return false;
                    }
                };
                resultados.add(pool.submit(intento));
            }
            salida.countDown();
            for (Future<Boolean> r : resultados) {
                if (r.get()) {
                    exitos++;
                } else {
                    conflictos++;
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(exitos).isEqualTo(1);
        assertThat(conflictos).isEqualTo(HILOS - 1);
        assertThat(repositorio.findAll()).filteredOn(r -> r.getSala().getId().equals(salaId)).hasSize(1);
    }
}
