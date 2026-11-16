package com.josue.reservas.reserva;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping
    public List<ReservaResponse> listar() {
        return servicio.listar();
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
