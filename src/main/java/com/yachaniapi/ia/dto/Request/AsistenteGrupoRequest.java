package com.yachaniapi.ia.dto.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// Pedido al asistente IA de "Crear grupo": lo que escribe el tutor en el chat, lo que ya
// lleva llenado en el formulario y los últimos mensajes de la conversación.
public record AsistenteGrupoRequest(
        @NotBlank(message = "Escribe qué necesitas para tu grupo")
        @Size(max = 500, message = "El mensaje admite hasta 500 caracteres")
        String pedido,
        @Valid
        Formulario formulario,
        @Size(max = 10, message = "La conversación enviada admite hasta 10 mensajes")
        List<@Valid Mensaje> historial
) {
    public record Formulario(
            @Size(max = 100) String nombre,
            @Size(max = 500) String descripcion,
            @Size(max = 50) String metodoEstudio,
            Integer capacidadMaxima
    ) {}

    public record Mensaje(
            @Pattern(regexp = "USUARIO|ASISTENTE", message = "El rol debe ser USUARIO o ASISTENTE")
            String rol,
            @NotBlank @Size(max = 1000) String texto
    ) {}
}
