package com.josue.reservas.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.josue.reservas.comun.ConflictoException;
import com.josue.reservas.comun.DatosInvalidosException;
import com.josue.reservas.sala.Sala;
import com.josue.reservas.sala.SalaService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Reglas de negocio de ReservaService, aisladas de Spring y de la base de datos. */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final LocalDateTime DIEZ = LocalDateTime.of(2030, 3, 4, 10, 0);
    private static final LocalDateTime ONCE = DIEZ.plusHours(1);

    @Mock
    ReservaRepository reservas;

    @Mock
    SalaService salas;

    @InjectMocks
    ReservaService servicio;

    Sala sala;

    @BeforeEach
    void setUp() {
        sala = new Sala("Azul", 8);
        ReflectionTestUtils.setField(sala, "id", 1L);
    }

    @Test
    void creaLaReservaSiLaSalaEstaLibre() {
        when(salas.buscarParaReservar(1L)).thenReturn(sala);
        when(reservas.salaOcupada(1L, DIEZ, ONCE)).thenReturn(false);
        when(reservas.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse respuesta = servicio.crear(new CrearReservaRequest(1L, "  Ana  ", DIEZ, ONCE));

        assertThat(respuesta.salaNombre()).isEqualTo("Azul");
        assertThat(respuesta.responsable()).isEqualTo("Ana"); // se recortan los espacios
        assertThat(respuesta.estado()).isEqualTo(EstadoReserva.ACTIVA);
    }

    @Test
    void rechazaConConflictoSiLaSalaYaEstaOcupada() {
        when(salas.buscarParaReservar(1L)).thenReturn(sala);
        when(reservas.salaOcupada(1L, DIEZ, ONCE)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new CrearReservaRequest(1L, "Ana", DIEZ, ONCE)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya está reservada");
        verify(reservas, never()).save(any());
    }

    @Test
    void rechazaQueElFinNoSeaPosteriorAlInicio() {
        assertThatThrownBy(() -> servicio.crear(new CrearReservaRequest(1L, "Ana", DIEZ, DIEZ)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("debe ser posterior a inicio")
                .extracting("campo").isEqualTo("fin");
        verify(salas, never()).buscarParaReservar(any()); // valida antes de tocar la BD
    }

    @Test
    void rechazaReservasDeMasDeDoceHoras() {
        assertThatThrownBy(() -> servicio.crear(new CrearReservaRequest(1L, "Ana", DIEZ, DIEZ.plusHours(13))))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("12 horas");
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        Reserva reserva = new Reserva(sala, "Ana", DIEZ, ONCE);
        when(reservas.findById(7L)).thenReturn(Optional.of(reserva));

        assertThat(servicio.cancelar(7L).estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThatThrownBy(() -> servicio.cancelar(7L)).isInstanceOf(ConflictoException.class);
    }
}
