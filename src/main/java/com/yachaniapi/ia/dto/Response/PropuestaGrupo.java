package com.yachaniapi.ia.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

// Propuesta editable de un grupo de tutoría (US22). Es la salida de POST /ia/tutorias/propuesta
// y la entrada de POST /ia/tutorias/ajuste (el formulario tal como lo tiene el tutor).
// En "cambios" del ajuste solo vienen los campos que cambian: los null no se envían.
// Los límites de la entrada son holgados; los de la salida los aplica PropuestaTutoriaService.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropuestaGrupo(
        @Size(max = 200) String nombre,
        @Size(max = 1000) String descripcion,
        @Size(max = 50) String metodoEstudio,
        Integer capacidadMaxima,
        @Size(max = 10) List<@Valid TemaPropuesto> temas,
        @Size(max = 20) List<@Valid SesionPropuesta> planSesiones
) {
    public record TemaPropuesto(@Size(max = 200) String nombre, Integer orden) {}

    // Se conecta con SesionInicialRequest de "Crear grupo": titulo → tema de la sesión,
    // objetivo → descripción y duracionMinutos → duración. La fecha la elige el tutor.
    public record SesionPropuesta(
            Integer numero,
            @Size(max = 300) String titulo,
            @Size(max = 200) String tema,
            @Size(max = 1000) String objetivo,
            Integer duracionMinutos
    ) {}
}
