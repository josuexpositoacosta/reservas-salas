package com.josue.reservas.comun;

/** Validación que depende de varios campos; se traduce a 400 con el mismo formato que Bean Validation. */
public class DatosInvalidosException extends RuntimeException {

    private final String campo;

    public DatosInvalidosException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
