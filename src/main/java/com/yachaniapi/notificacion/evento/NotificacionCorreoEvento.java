package com.yachaniapi.notificacion.evento;

public record NotificacionCorreoEvento(
        Long idNotificacion,
        String correoDestinatario,
        String titulo,
        String mensaje
) {
}