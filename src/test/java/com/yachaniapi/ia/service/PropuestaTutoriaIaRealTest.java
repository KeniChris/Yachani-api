package com.yachaniapi.ia.service;

import com.yachaniapi.ia.dto.Request.AjusteTutoriaRequest;
import com.yachaniapi.ia.dto.Request.PropuestaTutoriaRequest;
import com.yachaniapi.ia.dto.Response.EstadoPropuesta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Llama a OpenAI de verdad con la configuración compartida (application.yaml + ClienteIa).
// Desactivada por defecto: Leicy controla la clave y el consumo. Para correrla:
//   AI_API_KEY=... ./mvnw test -Dtest=PropuestaTutoriaIaRealTest
@EnabledIfEnvironmentVariable(named = "AI_API_KEY", matches = ".+")
@SpringBootTest(
        classes = PropuestaTutoriaIaRealTest.Aplicacion.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.model.chat=openai")
class PropuestaTutoriaIaRealTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
    })
    @Import({ClienteIa.class, PropuestaTutoriaService.class})
    static class Aplicacion {}

    @Autowired PropuestaTutoriaService service;

    @Test
    void generaYAjustaUnaPropuestaConLaApiReal() {
        var generada = service.generar(new PropuestaTutoriaRequest("Cálculo I",
                "Preparar a los estudiantes para el primer parcial",
                List.of("Límites", "Derivadas"), "Resolución de ejercicios", 15, null));

        assertEquals(EstadoPropuesta.COMPLETA, generada.estado());
        var propuesta = generada.propuesta();
        assertFalse(propuesta.nombre().isBlank());
        assertTrue(propuesta.nombre().length() <= 100);
        assertTrue(propuesta.descripcion().length() <= 500);
        assertTrue(propuesta.temas().size() <= 3);
        assertEquals(15, propuesta.capacidadMaxima());

        var ajuste = service.ajustar(new AjusteTutoriaRequest(
                "Haz la descripción más corta, de una sola oración", propuesta, List.of()));

        assertFalse(ajuste.respuesta().isBlank());
        assertNull(ajuste.cambios().nombre());
        assertNull(ajuste.cambios().capacidadMaxima());
    }
}
