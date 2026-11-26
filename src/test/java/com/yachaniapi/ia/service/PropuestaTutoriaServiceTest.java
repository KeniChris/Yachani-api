package com.yachaniapi.ia.service;

import com.yachaniapi.ia.dto.Request.AjusteTutoriaRequest;
import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest.MensajeAsistente;
import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest.RolMensaje;
import com.yachaniapi.ia.dto.Request.PropuestaTutoriaRequest;
import com.yachaniapi.ia.dto.Response.EstadoPropuesta;
import com.yachaniapi.ia.dto.Response.FaltantePropuesta;
import com.yachaniapi.ia.dto.Response.PropuestaGrupo;
import com.yachaniapi.ia.dto.Response.PropuestaGrupo.SesionPropuesta;
import com.yachaniapi.ia.dto.Response.PropuestaGrupo.TemaPropuesto;
import com.yachaniapi.ia.service.PropuestaTutoriaService.AjusteIa;
import com.yachaniapi.ia.service.PropuestaTutoriaService.PropuestaIa;
import com.yachaniapi.ia.service.PropuestaTutoriaService.SesionIa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// El ChatClient de Spring AI está simulado: se usa el ClienteIa real (la conexión de Leicy)
// sin llamar a OpenAI.
class PropuestaTutoriaServiceTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private ObjectProvider<ChatClient.Builder> proveedor;
    private ChatClient.Builder builder;
    private ChatClient.ChatClientRequestSpec peticion;
    private ChatClient.CallResponseSpec llamada;
    private PropuestaTutoriaService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        proveedor = mock(ObjectProvider.class);
        builder = mock(ChatClient.Builder.class);
        ChatClient chat = mock(ChatClient.class);
        peticion = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        llamada = mock(ChatClient.CallResponseSpec.class);
        when(proveedor.getIfAvailable()).thenReturn(builder);
        when(builder.build()).thenReturn(chat);
        when(chat.prompt()).thenReturn(peticion);
        when(peticion.call()).thenReturn(llamada);
        service = new PropuestaTutoriaService(new ClienteIa(proveedor), json);
    }

    @Test
    void generaUnaPropuestaCompletaYAplicaLasReglasDespuesDeLaIa() {
        responderIa(PropuestaIa.class, new PropuestaIa(
                "  " + "C".repeat(120) + "  ",
                "Repasamos límites y derivadas con ejercicios guiados.",
                List.of("Límites", "Derivadas", "límites", " ", "Integrales", "Series"),
                List.of(new SesionIa("Límites desde cero", "Límites", "Entender la idea de límite", 5),
                        new SesionIa(" ", "Derivadas", "Sin título", 60),
                        new SesionIa("Derivadas en la práctica", "Derivadas", "Resolver ejercicios", null),
                        new SesionIa("Maratón", "Integrales", "Repaso final", 900))));

        var respuesta = service.generar(new PropuestaTutoriaRequest(" Cálculo I ", "Aprobar el parcial",
                List.of("Límites", "Derivadas"), "Práctico", 12, "Que sea para principiantes"));

        assertEquals(EstadoPropuesta.COMPLETA, respuesta.estado());
        assertTrue(respuesta.faltantes().isEmpty());
        var propuesta = respuesta.propuesta();
        assertEquals("C".repeat(100), propuesta.nombre());
        assertEquals("Práctico", propuesta.metodoEstudio());
        assertEquals(12, propuesta.capacidadMaxima());
        assertEquals(List.of(new TemaPropuesto("Límites", 1), new TemaPropuesto("Derivadas", 2),
                new TemaPropuesto("Integrales", 3)), propuesta.temas());
        assertEquals(List.of(
                new SesionPropuesta(1, "Límites desde cero", "Límites", "Entender la idea de límite", 15),
                new SesionPropuesta(2, "Derivadas en la práctica", "Derivadas", "Resolver ejercicios", 60),
                new SesionPropuesta(3, "Maratón", "Integrales", "Repaso final", 480)), propuesta.planSesiones());

        // El modelo recibe los datos del tutor como JSON y las reglas en el mensaje de sistema.
        verify(peticion).system(PropuestaTutoriaService.INSTRUCCIONES_PROPUESTA);
        JsonNode entrada = entradaEnviada();
        assertEquals("Cálculo I", entrada.get("curso").asString());
        assertEquals(12, entrada.get("aforo").asInt());
    }

    @Test
    void unaSolicitudIncompletaDiceQueFaltaSinLlamarAOpenAi() {
        var respuesta = service.generar(new PropuestaTutoriaRequest(
                " ", "Aprobar el parcial", null, "Bailar", 1, null));

        assertEquals(EstadoPropuesta.INCOMPLETA, respuesta.estado());
        assertNull(respuesta.propuesta());
        assertEquals(List.of("curso", "metodoEstudio", "aforo"),
                respuesta.faltantes().stream().map(FaltantePropuesta::campo).toList());
        verifyNoInteractions(proveedor, builder, peticion, llamada);
    }

    @Test
    void faltarElAforoDevuelveElMensajeDelContrato() {
        var respuesta = service.generar(new PropuestaTutoriaRequest(
                "Cálculo I", "Aprobar el parcial", List.of(), "Práctico", null, null));

        assertEquals(List.of(new FaltantePropuesta("aforo", "Indica cuántos integrantes tendrá el grupo.")),
                respuesta.faltantes());
        verifyNoInteractions(proveedor);
    }

    @Test
    void unAjusteQueCambiaSoloLaDescripcionDevuelveSoloEseCampo() {
        responderIa(AjusteIa.class, new AjusteIa("Listo, hice la descripción más corta.",
                "Dominando Cálculo", " Repaso corto de derivadas. ", "", 0,
                false, List.of(), false, List.of()));

        var respuesta = service.ajustar(new AjusteTutoriaRequest("Hazla más corta", propuestaActual(), List.of()));

        assertEquals("Listo, hice la descripción más corta.", respuesta.respuesta());
        assertEquals(new PropuestaGrupo(null, "Repaso corto de derivadas.", null, null, null, null),
                respuesta.cambios());
        assertEquals(EstadoPropuesta.COMPLETA, respuesta.estado());
        // En JSON, "cambios" no trae los campos que no cambian.
        JsonNode cambios = json.readTree(json.writeValueAsString(respuesta)).get("cambios");
        assertEquals(List.of("descripcion"), cambios.propertyNames().stream().toList());
    }

    @Test
    void unAjusteDescartaMetodosInvalidosYFuerzaLaCapacidadMinima() {
        responderIa(AjusteIa.class, new AjusteIa("Cambié el método y la capacidad.",
                "", "", "Karaoke", 1, true,
                List.of("Derivadas", "Integrales", "Series", "Límites"), false, List.of()));

        var respuesta = service.ajustar(new AjusteTutoriaRequest("Cambia todo", propuestaActual(), null));

        assertNull(respuesta.cambios().metodoEstudio());
        assertEquals(2, respuesta.cambios().capacidadMaxima());
        assertEquals(3, respuesta.cambios().temas().size());
    }

    @Test
    void unPedidoFueraDeAlcanceSoloResponde() {
        responderIa(AjusteIa.class, new AjusteIa("Solo puedo ayudarte con la propuesta de tu grupo.",
                "", "", "", 0, false, List.of(), false, List.of()));

        var respuesta = service.ajustar(new AjusteTutoriaRequest("¿Quién ganó el partido?", propuestaActual(), null));

        assertEquals(new PropuestaGrupo(null, null, null, null, null, null), respuesta.cambios());
        assertEquals("Solo puedo ayudarte con la propuesta de tu grupo.", respuesta.respuesta());
    }

    @Test
    void unaRespuestaInvalidaDeLaIaResponde503() {
        // 1) El modelo devuelve algo que no se puede convertir al formato pedido.
        when(llamada.entity(eq(PropuestaIa.class), any())).thenThrow(new IllegalStateException("JSON inválido"));
        var conversion = assertThrows(ResponseStatusException.class, () -> service.generar(completa()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, conversion.getStatusCode());

        // 2) Devuelve una propuesta sin nombre.
        reset(llamada);
        responderIa(PropuestaIa.class, new PropuestaIa(" ", "Descripción", List.of(), List.of()));
        var sinNombre = assertThrows(ResponseStatusException.class, () -> service.generar(completa()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, sinNombre.getStatusCode());
        assertEquals("La IA devolvió una propuesta incompleta. Inténtalo de nuevo en unos minutos.",
                sinNombre.getReason());

        // 3) Un ajuste sin respuesta para el chat.
        responderIa(AjusteIa.class, new AjusteIa("", "Otro nombre", "", "", 0, false, List.of(), false, List.of()));
        var sinRespuesta = assertThrows(ResponseStatusException.class,
                () -> service.ajustar(new AjusteTutoriaRequest("Cambia el nombre", propuestaActual(), null)));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, sinRespuesta.getStatusCode());
    }

    @Test
    void sinIaConfiguradaResponde503() {
        when(proveedor.getIfAvailable()).thenReturn(null);

        var error = assertThrows(ResponseStatusException.class, () -> service.generar(completa()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void elHistorialSeRecortaALosUltimosDiezMensajes() {
        responderIa(AjusteIa.class, new AjusteIa("Listo.", "", "", "", 0, false, List.of(), false, List.of()));
        var historial = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new MensajeAsistente(i % 2 == 1 ? RolMensaje.USUARIO : RolMensaje.ASISTENTE,
                        "mensaje " + i))
                .toList();

        service.ajustar(new AjusteTutoriaRequest("Hazla más corta", propuestaActual(), historial));

        JsonNode enviado = entradaEnviada().get("historial");
        assertEquals(10, enviado.size());
        assertEquals("mensaje 3", enviado.get(0).get("texto").asString());
        assertEquals("mensaje 12", enviado.get(9).get("texto").asString());
        verify(peticion).system(PropuestaTutoriaService.INSTRUCCIONES_AJUSTE);
    }

    private <T> void responderIa(Class<T> tipo, T respuesta) {
        when(llamada.entity(eq(tipo), any())).thenReturn(respuesta);
    }

    @SuppressWarnings("unchecked")
    private JsonNode entradaEnviada() {
        var mensajes = ArgumentCaptor.forClass(Message.class);
        verify(peticion).messages(mensajes.capture());
        return json.readTree(mensajes.getValue().getText());
    }

    private static PropuestaTutoriaRequest completa() {
        return new PropuestaTutoriaRequest("Cálculo I", "Aprobar el parcial", List.of("Derivadas"),
                "Práctico", 12, null);
    }

    private static PropuestaGrupo propuestaActual() {
        return new PropuestaGrupo("Dominando Cálculo", "Un espacio para repasar derivadas paso a paso.",
                "Práctico", 12, List.of(new TemaPropuesto("Derivadas", 1)),
                List.of(new SesionPropuesta(1, "Derivadas básicas", "Derivadas", "Practicar reglas", 60)));
    }
}
