package com.yachaniapi.chat.evento;

import com.yachaniapi.chat.dto.MensajeChatResponse;

// Aviso que se publica en /topic/grupos/{idGrupo}/mensajes cuando cambia el chat.
// CREADO lleva el mensaje completo; ELIMINADO solo el id del mensaje borrado.
public record EventoChat(String tipo, Long idGrupo, MensajeChatResponse mensaje, Long idMensaje) {

    public static EventoChat creado(MensajeChatResponse mensaje) {
        return new EventoChat("CREADO", mensaje.getIdGrupo(), mensaje, mensaje.getIdMensaje());
    }

    public static EventoChat eliminado(Long idGrupo, Long idMensaje) {
        return new EventoChat("ELIMINADO", idGrupo, null, idMensaje);
    }
}
