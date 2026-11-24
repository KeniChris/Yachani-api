package com.yachaniapi.ia;

import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest;
import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest.Formulario;
import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest.Mensaje;
import com.yachaniapi.ia.service.AsistenteGrupoService;
import com.yachaniapi.ia.service.ClienteIa;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AsistenteGrupoServiceTest {

    private final ClienteIa cliente = mock(ClienteIa.class);
    private final AsistenteGrupoService servicio = new AsistenteGrupoService(
            cliente, Validation.buildDefaultValidatorFactory().getValidator());

    private final Formulario formulario = new Formulario(
            "Ingeniería de Software: repasos",
            "Un espacio para repasar conceptos.",
            "Resolución de ejercicios",
            12);

    @Test
    void soloSugiereLosCamposQueCambian() {
        responderConIa("Listo. Te sugerí un cambio en la descripción.",
                "Ingeniería de Software: repasos", "Una descripción mejorada.", "", "");

        var respuesta = servicio.responder(
                new AsistenteGrupoRequest("Mejora mi descripción de grupo", formulario, List.of()));

        assertEquals("Listo. Te sugerí un cambio en la descripción.", respuesta.respuesta());
        assertNull(respuesta.sugerencias().nombre(), "Un nombre igual al actual no es sugerencia");
        assertEquals("Una descripción mejorada.", respuesta.sugerencias().descripcion());
        assertNull(respuesta.sugerencias().metodoEstudio());
        assertNull(respuesta.sugerencias().capacidadMaxima());
    }

    @Test
    void descartaValoresQueNoCumplenLasReglasDelGrupo() {
        responderConIa("Hecho.", "x".repeat(101), "", "Bailar", "1");

        var sugerencias = servicio.responder(
                new AsistenteGrupoRequest("Cambia todo", formulario, null)).sugerencias();

        assertTrue(sugerencias.vacias());
    }

    @Test
    void aceptaMetodoYCapacidadValidos() {
        responderConIa("Listo.", "", "", "Debate", "20");

        var sugerencias = servicio.responder(
                new AsistenteGrupoRequest("Que sea de debate para 20", formulario, null)).sugerencias();

        assertEquals("Debate", sugerencias.metodoEstudio());
        assertEquals(20, sugerencias.capacidadMaxima());
    }

    @Test
    void sinTextoDeLaIaDaUnaRespuestaPorDefecto() {
        responderConIa("  ", "", "", "", "");

        var respuesta = servicio.responder(new AsistenteGrupoRequest("Hola", null, null));

        assertFalse(respuesta.respuesta().isBlank());
        assertTrue(respuesta.sugerencias().vacias());
    }

    @Test
    void enviaAlModeloElFormularioLaConversacionYElPedido() {
        responderConIa("Listo.", "", "", "", "");
        var historial = List.of(
                new Mensaje("USUARIO", "Mejora mi descripción"),
                new Mensaje("ASISTENTE", "Te sugerí un cambio."));

        servicio.responder(new AsistenteGrupoRequest("Hazla más corta", formulario, historial));

        ArgumentCaptor<String> entrada = ArgumentCaptor.forClass(String.class);
        verify(cliente).generar(anyString(), entrada.capture(), any());
        String texto = entrada.getValue();
        assertTrue(texto.contains("Ingeniería de Software: repasos"));
        assertTrue(texto.contains("Capacidad máxima: 12"));
        assertTrue(texto.contains("Tutor: Mejora mi descripción"));
        assertTrue(texto.contains("Asistente: Te sugerí un cambio."));
        assertTrue(texto.endsWith("Pedido del tutor: Hazla más corta"));
    }

    // CamposIa es package-private: se crea por reflexión con los valores que "devuelve" la IA.
    private void responderConIa(String respuesta, String nombre, String descripcion,
                                String metodo, String capacidad) {
        when(cliente.generar(anyString(), anyString(), any())).thenAnswer(invocacion -> {
            Class<?> tipo = invocacion.getArgument(2);
            Class<?>[] parametros = java.util.Arrays.stream(tipo.getRecordComponents())
                    .map(RecordComponent::getType).toArray(Class<?>[]::new);
            var constructor = tipo.getDeclaredConstructor(parametros);
            constructor.setAccessible(true);
            return constructor.newInstance(respuesta, nombre, descripcion, metodo, capacidad);
        });
    }
}
