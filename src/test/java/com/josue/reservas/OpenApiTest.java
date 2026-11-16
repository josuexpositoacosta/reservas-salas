package com.josue.reservas;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** La documentación es pública, describe todos los endpoints y marca como protegidos los de escritura. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void laDocumentacionOpenApiEsPublicaYDescribeLaSeguridad() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Reserva de salas"))
                .andExpect(jsonPath("$.paths['/api/reservas'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/reservas'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/reservas'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/reservas'].get.parameters[*].name", hasItem("page")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));

        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
