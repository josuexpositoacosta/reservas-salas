package com.josue.reservas;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Prueba la API de punta a punta: filtros de seguridad, validación, controladores, servicio,
 * JPA y migraciones Flyway sobre H2. Cada test crea su propia sala (nombre único) para no
 * depender del orden de ejecución ni de los datos de otros tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiReservasIntegrationTest {

    @Autowired
    MockMvc mvc;

    String token;

    @BeforeEach
    void login() throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"admin","clave":"admin123"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(respuesta, "$.token");
    }

    @Test
    void lasEscriturasExigenTokenYLasConsultasNo() throws Exception {
        mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sin token","capacidad":4}"""))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reservas/1/cancelacion"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"admin","clave":"mala"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));

        mvc.perform(get("/api/salas")).andExpect(status().isOk());
        mvc.perform(get("/api/reservas")).andExpect(status().isOk());
    }

    @Test
    void devuelve409SiLaSalaYaEstaOcupadaEnEseHorario() throws Exception {
        long sala = crearSala();
        reservar(sala, "2030-05-10T10:00:00", "2030-05-10T11:00:00")
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        // Solapamiento parcial y total -> 409
        reservar(sala, "2030-05-10T10:30:00", "2030-05-10T11:30:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("ya está reservada")));
        reservar(sala, "2030-05-10T09:00:00", "2030-05-10T12:00:00")
                .andExpect(status().isConflict());

        // Consecutiva (empieza justo cuando acaba la otra) -> permitida
        reservar(sala, "2030-05-10T11:00:00", "2030-05-10T12:00:00")
                .andExpect(status().isCreated());
    }

    @Test
    void cancelarLiberaElHueco() throws Exception {
        long sala = crearSala();
        String creada = reservar(sala, "2030-06-01T10:00:00", "2030-06-01T11:00:00")
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(creada, "$.id");

        mvc.perform(post("/api/reservas/{id}/cancelacion", id).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(post("/api/reservas/{id}/cancelacion", id).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isConflict());

        reservar(sala, "2030-06-01T10:00:00", "2030-06-01T11:00:00")
                .andExpect(status().isCreated());
    }

    @Test
    void devuelve400ConElDetalleDeCadaCampoInvalido() throws Exception {
        mvc.perform(post("/api/reservas").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salaId":null,"responsable":"","inicio":"2020-01-01T10:00:00","fin":null}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores", hasSize(4)))
                .andExpect(jsonPath("$.errores[?(@.campo=='inicio')].mensaje").value("debe ser una fecha futura"));

        long sala = crearSala();
        reservar(sala, "2030-07-01T11:00:00", "2030-07-01T10:00:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("fin"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("debe ser posterior a inicio"));

        reservar(999_999, "2030-07-01T10:00:00", "2030-07-01T11:00:00")
                .andExpect(status().isNotFound());
    }

    @Test
    void filtraPorSalaYFechaYPagina() throws Exception {
        long sala = crearSala();
        long otraSala = crearSala();
        reservar(sala, "2030-08-01T09:00:00", "2030-08-01T10:00:00");
        reservar(sala, "2030-08-01T11:00:00", "2030-08-01T12:00:00");
        reservar(sala, "2030-08-01T23:00:00", "2030-08-02T01:00:00"); // cruza la medianoche
        reservar(sala, "2030-08-03T09:00:00", "2030-08-03T10:00:00");
        reservar(otraSala, "2030-08-01T09:00:00", "2030-08-01T10:00:00");

        mvc.perform(get("/api/reservas").param("salaId", String.valueOf(sala)).param("fecha", "2030-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[0].inicio").value("2030-08-01T09:00:00"));

        // La que empieza el día 1 a las 23:00 también ocupa el día 2
        mvc.perform(get("/api/reservas").param("salaId", String.valueOf(sala)).param("fecha", "2030-08-02"))
                .andExpect(jsonPath("$.totalElementos").value(1));

        mvc.perform(get("/api/reservas").param("salaId", String.valueOf(sala))
                        .param("size", "2").param("page", "1").param("sort", "inicio,asc"))
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].inicio").value("2030-08-01T23:00:00"))
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.ultima").value(true));

        mvc.perform(get("/api/reservas").param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    // --- utilidades

    private String bearer() {
        return "Bearer " + token;
    }

    private long crearSala() throws Exception {
        String nombre = "Sala " + UUID.randomUUID().toString().substring(0, 8);
        String respuesta = mvc.perform(post("/api/salas").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"capacidad\":6}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private ResultActions reservar(long salaId, String inicio, String fin) throws Exception {
        return mvc.perform(post("/api/reservas").header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"salaId":%d,"responsable":"Ana","inicio":"%s","fin":"%s"}""".formatted(salaId, inicio, fin)));
    }
}
