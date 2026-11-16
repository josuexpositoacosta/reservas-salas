package com.josue.reservas.sala;

public record SalaResponse(Long id, String nombre, int capacidad) {

    static SalaResponse de(Sala sala) {
        return new SalaResponse(sala.getId(), sala.getNombre(), sala.getCapacidad());
    }
}
