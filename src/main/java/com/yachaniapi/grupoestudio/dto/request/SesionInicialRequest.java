package com.yachaniapi.grupoestudio.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// Sesión del plan que el tutor aprueba al crear el grupo. Usa las mismas validaciones que
// com.yachaniapi.sesion.dto.SesionRequest; la fecha futura la valida SesionEstudioService.
public record SesionInicialRequest(
        @NotBlank(message = "Datos inválidos: el tema es obligatorio")
        @Size(max = 150, message = "Datos inválidos: el tema no puede superar los 150 caracteres")
        String tema,

        @Size(max = 500, message = "Datos inválidos: la descripción no puede superar los 500 caracteres")
        String descripcion,

        @NotNull(message = "Datos inválidos: la fecha y hora son obligatorias")
        LocalDateTime fechaHoraInicio,

        @NotNull(message = "Datos inválidos: la duración es obligatoria")
        @Min(value = 15, message = "Datos inválidos: la duración mínima es de 15 minutos")
        @Max(value = 480, message = "Datos inválidos: la duración máxima es de 8 horas")
        Integer duracionMinutos,

        @Size(max = 200, message = "Datos inválidos: el lugar no puede superar los 200 caracteres")
        String lugar
) {}
