package com.josue.reservas.sala;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * SELECT ... FOR UPDATE sobre la sala: serializa las reservas concurrentes de una misma sala
     * para que dos peticiones simultáneas no pasen a la vez la comprobación de solapamiento.
     * Salas distintas no se bloquean entre sí.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sala s where s.id = :id")
    Optional<Sala> buscarParaReservar(Long id);
}
