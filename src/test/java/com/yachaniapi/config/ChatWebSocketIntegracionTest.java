package com.yachaniapi.config;

import com.yachaniapi.chat.dto.MensajeChatResponse;
import com.yachaniapi.chat.evento.EventoChat;
import com.yachaniapi.chat.service.ChatTiempoRealListener;
import com.yachaniapi.grupoestudio.service.AccesoGrupoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Levanta un servidor real con la seguridad y el WebSocket del proyecto (sin base de datos)
// y se conecta como lo haría el navegador desde el front: handshake con Origin, JWT en el
// CONNECT, suscripción al chat del grupo y recepción del aviso en JSON.
@SpringBootTest(
        classes = ChatWebSocketIntegracionTest.Aplicacion.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "JWT_SECRET=una-clave-de-prueba-con-mas-de-32-bytes!!",
                "app.cors.allowed-origins=http://localhost:4200",
        })
class ChatWebSocketIntegracionTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
    })
    @Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class, WebSocketConfig.class,
            WebSocketAuthInterceptor.class, ChatTiempoRealListener.class})
    static class Aplicacion {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(Object t, TransactionDefinition d) { }
                @Override protected void doCommit(DefaultTransactionStatus s) { }
                @Override protected void doRollback(DefaultTransactionStatus s) { }
            };
        }
    }

    @LocalServerPort
    int puerto;
    @Autowired
    JwtEncoder encoder;
    @Autowired
    ChatTiempoRealListener listener;
    @MockitoBean
    AccesoGrupoService acceso;

    private final WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());

    @AfterEach
    void cerrar() {
        cliente.stop();
    }

    @Test
    void unMiembroRecibeElMensajeNuevoComoJson() throws Exception {
        StompSession sesion = conectar(token(7L));
        BlockingQueue<Map<String, Object>> recibidos = new LinkedBlockingQueue<>();
        sesion.subscribe("/topic/grupos/10/mensajes", manejador(recibidos));
        esperarSuscripcion();

        var mensaje = new MensajeChatResponse(55L, 10L, 7L, "Ana", "Hola",
                LocalDateTime.of(2026, 11, 16, 10, 30, 0));
        listener.alCambiarChat(EventoChat.creado(mensaje));

        Map<String, Object> evento = recibidos.poll(5, TimeUnit.SECONDS);
        assertNotNull(evento, "No llegó el aviso por el WebSocket");
        assertEquals("CREADO", evento.get("tipo"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cuerpo = (Map<String, Object>) evento.get("mensaje");
        assertEquals("Hola", cuerpo.get("contenido"));
        // Mismo formato de fecha que la API REST (texto ISO, no un arreglo de números).
        assertEquals("2026-11-16T10:30:00", cuerpo.get("fechaEnvio"));

        listener.alCambiarChat(EventoChat.eliminado(10L, 55L));
        Map<String, Object> borrado = recibidos.poll(5, TimeUnit.SECONDS);
        assertNotNull(borrado);
        assertEquals("ELIMINADO", borrado.get("tipo"));
        assertEquals(55, borrado.get("idMensaje"));
    }

    // El broker de WebSocket registra sus propios ejecutores; sin spring.task.execution.mode=force
    // Boot dejaba de crear applicationTaskExecutor y NotificacionCorreoListener no arrancaba.
    @Test
    void elWebSocketNoQuitaElEjecutorDeTareasDeLaAplicacion(
            @Autowired org.springframework.context.ApplicationContext contexto) {
        assertTrue(contexto.containsBean("applicationTaskExecutor"));
        assertInstanceOf(org.springframework.core.task.TaskExecutor.class,
                contexto.getBean("applicationTaskExecutor"));
    }

    @Test
    void sinTokenNoSePuedeConectar() {
        assertThrows(ExecutionException.class, () -> conectar(null));
    }

    @Test
    void quienNoEsDelGrupoNoRecibeNada() throws Exception {
        when(acceso.miembro(20L, false)).thenThrow(new AccessDeniedException("No perteneces a este grupo"));
        StompSession sesion = conectar(token(8L));
        BlockingQueue<Map<String, Object>> recibidos = new LinkedBlockingQueue<>();
        sesion.subscribe("/topic/grupos/20/mensajes", manejador(recibidos));
        esperarSuscripcion();

        listener.alCambiarChat(EventoChat.eliminado(20L, 1L));
        assertNull(recibidos.poll(1, TimeUnit.SECONDS));
    }

    private StompSession conectar(String token) throws Exception {
        cliente.setMessageConverter(new JacksonJsonMessageConverter());
        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        handshake.setOrigin("http://localhost:4200");
        StompHeaders connect = new StompHeaders();
        if (token != null) connect.add("Authorization", "Bearer " + token);
        return cliente.connectAsync("ws://localhost:" + puerto + "/api/ws", handshake, connect,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private StompFrameHandler manejador(BlockingQueue<Map<String, Object>> destino) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                destino.add((Map<String, Object>) payload);
            }
        };
    }

    // SUBSCRIBE no tiene confirmación en STOMP: se da un margen para que el servidor la registre.
    private void esperarSuscripcion() throws InterruptedException {
        Thread.sleep(300);
    }

    private String token(Long idUsuario) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(idUsuario.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("role", "ESTUDIANTE")
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
