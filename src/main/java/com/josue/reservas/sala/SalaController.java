package com.josue.reservas.sala;

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
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService servicio;

    public SalaController(SalaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<SalaResponse> crear(@Valid @RequestBody CrearSalaRequest datos) {
        SalaResponse sala = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/salas/" + sala.id())).body(sala);
    }

    @GetMapping
    public List<SalaResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public SalaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }
}
