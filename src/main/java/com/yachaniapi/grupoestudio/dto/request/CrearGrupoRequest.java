package com.yachaniapi.grupoestudio.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CrearGrupoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
        String descripcion,
        @NotBlank(message = "El método de estudio es obligatorio")
        @Size(max = 50, message = "El método admite hasta 50 caracteres")
        String metodoEstudio,
        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 2, message = "La capacidad mínima es de 2 estudiantes")
        Integer capacidadMaxima,
        @Positive(message = "El ID del creador debe ser positivo")
        Long idCreador,
        @Size(max = 3, message = "Agrega hasta 3 temas iniciales")
        List<@NotBlank(message = "El nombre del tema es obligatorio")
             @Size(max = 100, message = "El nombre del tema admite hasta 100 caracteres")
             String> temasIniciales,
        // Plan de sesiones aprobado al crear el grupo. Solo lo puede enviar un tutor.
        @Size(max = 10, message = "Agrega hasta 10 sesiones iniciales")
        List<@Valid SesionInicialRequest> sesionesIniciales
) {
    public CrearGrupoRequest(String nombre, String descripcion, String metodoEstudio,
                            Integer capacidadMaxima, Long idCreador) {
        this(nombre, descripcion, metodoEstudio, capacidadMaxima, idCreador, List.of(), List.of());
    }

    public CrearGrupoRequest(String nombre, String descripcion, String metodoEstudio,
                            Integer capacidadMaxima, Long idCreador, List<String> temasIniciales) {
        this(nombre, descripcion, metodoEstudio, capacidadMaxima, idCreador, temasIniciales, List.of());
    }
}
