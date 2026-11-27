package com.yachaniapi.notificacion.service;

import com.yachaniapi.notificacion.evento.NotificacionCorreoEvento;
import com.yachaniapi.usuario.exception.EnvioCorreoException;
import com.yachaniapi.usuario.service.CorreoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class NotificacionCorreoListener {

    private final CorreoService correoService;
    private final TaskExecutor ejecutor;
    private final String destinatarioPrueba;

    public NotificacionCorreoListener(
            CorreoService correoService,
            @Qualifier("applicationTaskExecutor") TaskExecutor ejecutor,
            @Value("${app.notificaciones.correo.destinatario-prueba:}") String destinatarioPrueba) {
        this.correoService = correoService;
        this.ejecutor = ejecutor;
        this.destinatarioPrueba = destinatarioPrueba.trim();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alGuardarNotificacion(NotificacionCorreoEvento evento) {
        String correo = evento.correoDestinatario();
        if (correo == null || correo.isBlank()) {
            return;
        }

        String destino = correo.trim();
        if (!destinatarioPrueba.isEmpty() && !destinatarioPrueba.equalsIgnoreCase(destino)) {
            return;
        }

        try {
            ejecutor.execute(() -> enviar(evento, destino));
        } catch (TaskRejectedException exception) {
            log.warn("No se pudo programar el correo de la notificación {}", evento.idNotificacion());
        }
    }

    private void enviar(NotificacionCorreoEvento evento, String destino) {
        try {
            correoService.enviarNotificacion(destino, evento.titulo(), evento.mensaje());
        } catch (EnvioCorreoException exception) {
            log.warn("Falló el correo de la notificación {}. {}",
                    evento.idNotificacion(), exception.getMessage());
        }
    }
}