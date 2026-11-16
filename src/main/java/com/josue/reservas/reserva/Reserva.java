package com.josue.reservas.reserva;

import com.josue.reservas.sala.Sala;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Column(nullable = false, length = 100)
    private String responsable;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    protected Reserva() {
        // requerido por JPA
    }

    public Reserva(Sala sala, String responsable, LocalDateTime inicio, LocalDateTime fin) {
        this.sala = sala;
        this.responsable = responsable;
        this.inicio = inicio;
        this.fin = fin;
        this.estado = EstadoReserva.ACTIVA;
    }

    /** Cancelar no borra la fila: se conserva el histórico y deja libre el hueco. */
    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public boolean estaCancelada() {
        return estado == EstadoReserva.CANCELADA;
    }

    public Long getId() {
        return id;
    }

    public Sala getSala() {
        return sala;
    }

    public String getResponsable() {
        return responsable;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public EstadoReserva getEstado() {
        return estado;
    }
}
