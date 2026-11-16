package com.josue.reservas.reserva;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import com.josue.reservas.comun.PaginaResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService servicio;

    public ReservaController(ReservaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody CrearReservaRequest datos) {
        ReservaResponse reserva = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/reservas/" + reserva.id())).body(reserva);
    }

    /**
     * Listado paginado con filtros opcionales.
     * Ej.: GET /api/reservas?salaId=1&fecha=2027-01-10&estado=ACTIVA&page=0&size=20&sort=inicio,asc
     */
    @GetMapping
    public PaginaResponse<ReservaResponse> listar(
            @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoReserva estado,
            @PageableDefault(size = 20, sort = "inicio") Pageable pagina) {
        return servicio.listar(new FiltroReservas(salaId, fecha, estado), pagina);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * Cancelar es un cambio de estado, no un borrado (la reserva sigue consultable),
     * por eso es un POST a una subruta y no un DELETE.
     */
    @PostMapping("/{id}/cancelacion")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return servicio.cancelar(id);
    }
}
