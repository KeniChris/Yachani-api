package com.yachaniapi.config;

import com.yachaniapi.grupoestudio.service.AccesoGrupoService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Seguridad del WebSocket. El navegador no deja poner cabeceras en el handshake, así que
// el JWT viaja en el frame CONNECT de STOMP (cabecera Authorization) y se valida aquí con
// el mismo JwtDecoder que usa la API REST.
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Pattern TOPICO_CHAT = Pattern.compile("^/topic/grupos/(\\d+)/mensajes$");

    private final JwtDecoder jwtDecoder;
    private final AccesoGrupoService acceso;
    private final TransactionTemplate transaccion;

    public WebSocketAuthInterceptor(
            JwtDecoder jwtDecoder,
            AccesoGrupoService acceso,
            PlatformTransactionManager transactionManager) {
        this.jwtDecoder = jwtDecoder;
        this.acceso = acceso;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.transaccion.setReadOnly(true);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        // Si las cabeceras no vienen mutables se leen de una copia: así ningún frame se
        // salta la validación por llegar en otro formato.
        boolean mutable = accessor != null;
        if (!mutable) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (accessor.getCommand() == null) {
            return message; // latidos (heartbeats)
        }
        switch (accessor.getCommand()) {
            case CONNECT -> {
                accessor.setUser(autenticar(accessor));
                if (!mutable) {
                    return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                }
            }
            case SUBSCRIBE -> validarSuscripcion(accessor);
            // Nadie publica por el socket: sin esto, un cliente podría mandar un SEND a
            // /topic/grupos/{id}/mensajes y el broker lo reenviaría a todo el grupo.
            case SEND -> throw new AccessDeniedException("Los mensajes se envían por la API REST");
            default -> { }
        }
        return message;
    }

    private JwtAuthenticationToken autenticar(StompHeaderAccessor accessor) {
        String cabecera = accessor.getFirstNativeHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new BadCredentialsException("Inicia sesión con un token válido");
        }
        try {
            return new JwtAuthenticationToken(jwtDecoder.decode(cabecera.substring(7).trim()));
        } catch (JwtException exception) {
            throw new BadCredentialsException("El token está ausente, vencido o no es válido");
        }
    }

    // Solo se puede escuchar el chat de un grupo al que se pertenece.
    private void validarSuscripcion(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof JwtAuthenticationToken usuario)) {
            throw new BadCredentialsException("Inicia sesión con un token válido");
        }
        String destino = accessor.getDestination();
        Matcher coincidencia = destino == null ? null : TOPICO_CHAT.matcher(destino);
        if (coincidencia == null || !coincidencia.matches()) {
            throw new AccessDeniedException("Destino no permitido");
        }
        Long idGrupo = Long.valueOf(coincidencia.group(1));
        // AccesoGrupoService lee al usuario del SecurityContext y recorre los participantes
        // (colección perezosa), por eso se ejecuta con el usuario puesto y en una transacción.
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(usuario);
        SecurityContextHolder.setContext(contexto);
        try {
            transaccion.executeWithoutResult(estado -> acceso.miembro(idGrupo, false));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
