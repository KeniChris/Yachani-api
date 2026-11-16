package com.yachaniapi.config;

import com.yachaniapi.grupoestudio.service.AccesoGrupoService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketAuthInterceptorTest {

    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final AccesoGrupoService acceso = mock(AccesoGrupoService.class);
    private final MessageChannel canal = mock(MessageChannel.class);
    private final WebSocketAuthInterceptor interceptor =
            new WebSocketAuthInterceptor(decoder, acceso, new TransaccionFalsa());

    @Test
    void conectarConTokenValidoIdentificaAlUsuario() {
        when(decoder.decode("abc")).thenReturn(jwt("7"));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer abc");
        accessor.setLeaveMutable(true);

        Message<?> resultado = interceptor.preSend(mensaje(accessor), canal);

        var usuario = StompHeaderAccessor.wrap(resultado).getUser();
        assertInstanceOf(JwtAuthenticationToken.class, usuario);
        assertEquals("7", usuario.getName());
    }

    @Test
    void conectarSinTokenOConTokenInvalidoSeRechaza() {
        StompHeaderAccessor sinToken = StompHeaderAccessor.create(StompCommand.CONNECT);
        sinToken.setLeaveMutable(true);
        assertThrows(BadCredentialsException.class, () -> interceptor.preSend(mensaje(sinToken), canal));

        when(decoder.decode("malo")).thenThrow(new BadJwtException("vencido"));
        StompHeaderAccessor tokenMalo = StompHeaderAccessor.create(StompCommand.CONNECT);
        tokenMalo.addNativeHeader("Authorization", "Bearer malo");
        tokenMalo.setLeaveMutable(true);
        assertThrows(BadCredentialsException.class, () -> interceptor.preSend(mensaje(tokenMalo), canal));
    }

    @Test
    void unMiembroPuedeSuscribirseAlChatDeSuGrupo() {
        var usuario = new JwtAuthenticationToken(jwt("7"));
        var usuarioEnContexto = new AtomicReference<Object>();
        when(acceso.miembro(10L, false)).thenAnswer(invocacion -> {
            usuarioEnContexto.set(SecurityContextHolder.getContext().getAuthentication());
            return null;
        });

        interceptor.preSend(suscripcion(usuario, "/topic/grupos/10/mensajes"), canal);

        verify(acceso).miembro(10L, false);
        assertSame(usuario, usuarioEnContexto.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void quienNoEsDelGrupoNoPuedeEscucharSuChat() {
        when(acceso.miembro(10L, false)).thenThrow(new AccessDeniedException("No perteneces a este grupo"));
        var usuario = new JwtAuthenticationToken(jwt("8"));

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(suscripcion(usuario, "/topic/grupos/10/mensajes"), canal));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void noSePuedeSuscribirSinSesionNiAOtrosDestinos() {
        assertThrows(BadCredentialsException.class,
                () -> interceptor.preSend(suscripcion(null, "/topic/grupos/10/mensajes"), canal));
        var usuario = new JwtAuthenticationToken(jwt("7"));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(suscripcion(usuario, "/topic/grupos/10/otra-cosa"), canal));
        verifyNoInteractions(acceso);
    }

    @Test
    void nadiePuedePublicarDirectamenteEnElSocket() {
        StompHeaderAccessor envio = StompHeaderAccessor.create(StompCommand.SEND);
        envio.setDestination("/topic/grupos/10/mensajes");
        envio.setUser(new JwtAuthenticationToken(jwt("7")));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(mensaje(envio), canal));
    }

    private Message<byte[]> suscripcion(JwtAuthenticationToken usuario, String destino) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        if (usuario != null) accessor.setUser(usuario);
        return mensaje(accessor);
    }

    private Message<byte[]> mensaje(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt jwt(String sujeto) {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject(sujeto)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    // Gestor de transacciones que no hace nada: basta para ejecutar el TransactionTemplate.
    private static class TransaccionFalsa extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
