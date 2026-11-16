package com.yachaniapi.chat.service;

import com.yachaniapi.chat.evento.EventoChat;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Reenvía los cambios del chat a quienes lo tienen abierto. Se ejecuta después del commit
// para no avisar de un mensaje que al final no se guardó (o de un borrado que se revirtió).
@Component
public class ChatTiempoRealListener {

    private final SimpMessagingTemplate mensajeria;

    public ChatTiempoRealListener(SimpMessagingTemplate mensajeria) {
        this.mensajeria = mensajeria;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarChat(EventoChat evento) {
        mensajeria.convertAndSend("/topic/grupos/" + evento.idGrupo() + "/mensajes", evento);
    }
}
