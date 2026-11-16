package com.josue.reservas.reserva;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionales del listado; los que vienen a null no se aplican. */
public record FiltroReservas(Long salaId, LocalDate fecha, EstadoReserva estado) {

    Specification<Reserva> aEspecificacion() {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (salaId != null) {
                condiciones.add(cb.equal(raiz.get("sala").get("id"), salaId));
            }
            if (fecha != null) {
                // Reservas que tocan ese día [00:00, 00:00 del día siguiente): incluye las que
                // empiezan el día anterior y terminan ese día.
                condiciones.add(cb.lessThan(raiz.get("inicio"), fecha.plusDays(1).atStartOfDay()));
                condiciones.add(cb.greaterThan(raiz.get("fin"), fecha.atStartOfDay()));
            }
            if (estado != null) {
                condiciones.add(cb.equal(raiz.get("estado"), estado));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
