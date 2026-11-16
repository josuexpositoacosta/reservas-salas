package com.josue.reservas.seguridad;

public record TokenResponse(String token, String tipo, long expiraEnSegundos) {
}
