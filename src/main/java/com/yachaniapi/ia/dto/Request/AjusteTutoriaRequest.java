package com.yachaniapi.ia.dto.Request;

import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest.MensajeAsistente;
import com.yachaniapi.ia.dto.Response.PropuestaGrupo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Pedido de ajuste sobre la propuesta actual. Del historial solo se envían a la IA los
// últimos 10 mensajes; se aceptan más para que el front no tenga que recortarlo.
public record AjusteTutoriaRequest(
        @NotBlank(message = "Escribe qué quieres ajustar de la propuesta")
        @Size(max = 500, message = "El mensaje admite hasta 500 caracteres")
        String mensaje,

        @NotNull(message = "Falta la propuesta actual")
        @Valid
        PropuestaGrupo propuesta,

        @Size(max = 50, message = "El historial admite hasta 50 mensajes")
        List<@Valid MensajeAsistente> historial
) {}
