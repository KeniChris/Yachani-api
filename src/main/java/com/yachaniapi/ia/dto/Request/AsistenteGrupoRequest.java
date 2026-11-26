package com.yachaniapi.ia.dto.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AsistenteGrupoRequest(
        @NotBlank(message = "Escribe qué necesitas del asistente")
        @Size(max = 500, message = "El pedido admite hasta 500 caracteres")
        String pedido,

        @NotNull(message = "Falta el formulario del grupo")
        @Valid
        FormularioGrupo formulario,

        @Size(max = 10, message = "El historial admite hasta 10 mensajes")
        List<@Valid MensajeAsistente> historial
) {

    public record FormularioGrupo(
            @Size(max = 200) String nombre,
            @Size(max = 1000) String descripcion,
            @Size(max = 50) String metodoEstudio,
            Integer capacidadMaxima
    ) {}

    public record MensajeAsistente(
            @NotNull RolMensaje rol,
            @NotBlank @Size(max = 2000) String texto
    ) {}

    public enum RolMensaje { USUARIO, ASISTENTE }
}