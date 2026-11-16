package com.josue.reservas.reserva;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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

@Tag(name = "Reservas")
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService servicio;

    public ReservaController(ReservaService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Reservar una sala", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Los horarios son intervalos [inicio, fin): una reserva de 11:00 a 12:00 no choca con otra de 10:00 a 11:00.")
    @ApiResponse(responseCode = "201", description = "Reserva creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (incluye fin anterior a inicio o más de 12 h)")
    @ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
    @ApiResponse(responseCode = "404", description = "La sala no existe")
    @ApiResponse(responseCode = "409", description = "La sala ya está reservada en ese horario")
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody CrearReservaRequest datos) {
        ReservaResponse reserva = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/reservas/" + reserva.id())).body(reserva);
    }

    /**
     * Listado paginado con filtros opcionales.
     * Ej.: GET /api/reservas?salaId=1&fecha=2027-01-10&estado=ACTIVA&page=0&size=20&sort=inicio,asc
     */
    @Operation(summary = "Listar reservas", description = "Filtros opcionales y combinables. "
            + "«fecha» devuelve las reservas que tocan ese día, aunque empiecen la noche anterior. "
            + "Orden permitido: id, inicio, fin, responsable, estado. Máximo 100 por página. No necesita token.")
    @ApiResponse(responseCode = "200", description = "Página de reservas")
    @ApiResponse(responseCode = "400", description = "Filtro u orden no válido")
    @GetMapping
    public PaginaResponse<ReservaResponse> listar(
            @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoReserva estado,
            @ParameterObject @PageableDefault(size = 20, sort = "inicio") Pageable pagina) {
        return servicio.listar(new FiltroReservas(salaId, fecha, estado), pagina);
    }

    @Operation(summary = "Ver una reserva")
    @ApiResponse(responseCode = "200", description = "La reserva")
    @ApiResponse(responseCode = "404", description = "No existe")
    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * Cancelar es un cambio de estado, no un borrado (la reserva sigue consultable),
     * por eso es un POST a una subruta y no un DELETE.
     */
    @Operation(summary = "Cancelar una reserva", security = @SecurityRequirement(name = "bearerAuth"),
            description = "No la borra: pasa a CANCELADA y el hueco queda libre.")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada")
    @ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
    @ApiResponse(responseCode = "404", description = "No existe")
    @ApiResponse(responseCode = "409", description = "Ya estaba cancelada")
    @PostMapping("/{id}/cancelacion")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return servicio.cancelar(id);
    }
}
