package com.josue.reservas.reserva;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long salaId,
        String salaNombre,
        String responsable,
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoReserva estado) {

    static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(), r.getSala().getId(), r.getSala().getNombre(),
                r.getResponsable(), r.getInicio(), r.getFin(), r.getEstado());
    }
}
