package com.josue.reservas.sala;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Salas")
@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService servicio;

    public SalaController(SalaService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Crear una sala", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Sala creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
    @ApiResponse(responseCode = "409", description = "Ya existe una sala con ese nombre")
    @PostMapping
    public ResponseEntity<SalaResponse> crear(@Valid @RequestBody CrearSalaRequest datos) {
        SalaResponse sala = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/salas/" + sala.id())).body(sala);
    }

    @Operation(summary = "Listar las salas", description = "Ordenadas por nombre. No necesita token.")
    @GetMapping
    public List<SalaResponse> listar() {
        return servicio.listar();
    }

    @Operation(summary = "Ver una sala")
    @ApiResponse(responseCode = "200", description = "La sala")
    @ApiResponse(responseCode = "404", description = "No existe")
    @GetMapping("/{id}")
    public SalaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }
}
