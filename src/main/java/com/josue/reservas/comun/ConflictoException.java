package com.josue.reservas.comun;

/** Se traduce a 409 Conflict: la petición es válida pero choca con el estado actual. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
