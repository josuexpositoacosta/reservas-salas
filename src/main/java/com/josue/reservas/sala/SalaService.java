package com.josue.reservas.sala;

import com.josue.reservas.comun.ConflictoException;
import com.josue.reservas.comun.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalaService {

    private final SalaRepository salas;

    public SalaService(SalaRepository salas) {
        this.salas = salas;
    }

    @Transactional
    public SalaResponse crear(CrearSalaRequest datos) {
        String nombre = datos.nombre().trim();
        if (salas.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una sala con el nombre '" + nombre + "'");
        }
        return SalaResponse.de(salas.save(new Sala(nombre, datos.capacidad())));
    }

    public List<SalaResponse> listar() {
        return salas.findAll(Sort.by("nombre")).stream().map(SalaResponse::de).toList();
    }

    public SalaResponse obtener(Long id) {
        return SalaResponse.de(buscar(id));
    }

    public Sala buscar(Long id) {
        return salas.findById(id).orElseThrow(() -> salaNoEncontrada(id));
    }

    /** Devuelve la sala bloqueada hasta el final de la transacción en curso (ver SalaRepository). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Sala buscarParaReservar(Long id) {
        return salas.buscarParaReservar(id).orElseThrow(() -> salaNoEncontrada(id));
    }

    private static RecursoNoEncontradoException salaNoEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la sala con id " + id);
    }
}
