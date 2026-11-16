package com.josue.reservas.reserva;

import com.josue.reservas.comun.ConflictoException;
import com.josue.reservas.comun.DatosInvalidosException;
import com.josue.reservas.comun.PaginaResponse;
import com.josue.reservas.comun.RecursoNoEncontradoException;
import com.josue.reservas.sala.Sala;
import com.josue.reservas.sala.SalaService;
import java.time.Duration;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservaService {

    static final Duration DURACION_MAXIMA = Duration.ofHours(12);

    private final ReservaRepository reservas;
    private final SalaService salas;

    public ReservaService(ReservaRepository reservas, SalaService salas) {
        this.reservas = reservas;
        this.salas = salas;
    }

    @Transactional
    public ReservaResponse crear(CrearReservaRequest datos) {
        if (!datos.fin().isAfter(datos.inicio())) {
            throw new DatosInvalidosException("fin", "debe ser posterior a inicio");
        }
        if (Duration.between(datos.inicio(), datos.fin()).compareTo(DURACION_MAXIMA) > 0) {
            throw new DatosInvalidosException("fin", "una reserva no puede durar más de " + DURACION_MAXIMA.toHours() + " horas");
        }
        Sala sala = salas.buscarParaReservar(datos.salaId());
        if (reservas.salaOcupada(sala.getId(), datos.inicio(), datos.fin())) {
            throw new ConflictoException("La sala '" + sala.getNombre() + "' ya está reservada entre "
                    + datos.inicio() + " y " + datos.fin());
        }
        Reserva reserva = new Reserva(sala, datos.responsable().trim(), datos.inicio(), datos.fin());
        return ReservaResponse.de(reservas.save(reserva));
    }

    /** Campos por los que se puede ordenar; cualquier otro es un 400, no un 500. */
    static final Set<String> CAMPOS_ORDENABLES = Set.of("id", "inicio", "fin", "responsable", "estado");

    public PaginaResponse<ReservaResponse> listar(FiltroReservas filtro, Pageable pagina) {
        pagina.getSort().forEach(orden -> {
            if (!CAMPOS_ORDENABLES.contains(orden.getProperty())) {
                throw new DatosInvalidosException("sort", "no se puede ordenar por '" + orden.getProperty()
                        + "'; valores permitidos: " + new TreeSet<>(CAMPOS_ORDENABLES));
            }
        });
        return PaginaResponse.de(reservas.findAll(filtro.aEspecificacion(), pagina), ReservaResponse::de);
    }

    public ReservaResponse obtener(Long id) {
        return ReservaResponse.de(buscar(id));
    }

    @Transactional
    public ReservaResponse cancelar(Long id) {
        Reserva reserva = buscar(id);
        if (reserva.estaCancelada()) {
            throw new ConflictoException("La reserva " + id + " ya está cancelada");
        }
        reserva.cancelar();
        return ReservaResponse.de(reserva);
    }

    private Reserva buscar(Long id) {
        return reservas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva con id " + id));
    }
}
