package com.josue.reservas.reserva;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    /** Carga la sala en la misma consulta (evita N+1 al construir ReservaResponse). */
    @Override
    @EntityGraph(attributePaths = "sala")
    Page<Reserva> findAll(Specification<Reserva> filtro, Pageable pagina);

    /**
     * Dos intervalos semiabiertos [inicio, fin) se solapan si cada uno empieza antes de que acabe el otro.
     * Así, 10:00-11:00 y 11:00-12:00 NO se solapan (reservas consecutivas permitidas).
     */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.sala.id = :salaId
              and r.estado = :estado
              and r.inicio < :fin
              and r.fin > :inicio
            """)
    boolean existeSolapamiento(Long salaId, LocalDateTime inicio, LocalDateTime fin, EstadoReserva estado);

    default boolean salaOcupada(Long salaId, LocalDateTime inicio, LocalDateTime fin) {
        return existeSolapamiento(salaId, inicio, fin, EstadoReserva.ACTIVA);
    }
}
