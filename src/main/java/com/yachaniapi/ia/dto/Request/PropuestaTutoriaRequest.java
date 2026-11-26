package com.yachaniapi.ia.dto.Request;

import jakarta.validation.constraints.Size;

import java.util.List;

// Datos del tutor para generar la propuesta. Curso, objetivo, método y aforo son obligatorios,
// pero no se validan con @NotBlank: si faltan, el servicio responde INCOMPLETA con "faltantes"
// (sin llamar a la IA) en lugar de un 400.
public record PropuestaTutoriaRequest(
        @Size(max = 100, message = "El curso admite hasta 100 caracteres")
        String curso,
        @Size(max = 500, message = "El objetivo admite hasta 500 caracteres")
        String objetivo,
        @Size(max = 10, message = "Indica hasta 10 temas clave")
        List<@Size(max = 100, message = "Cada tema clave admite hasta 100 caracteres") String> temasClave,
        @Size(max = 50, message = "El método admite hasta 50 caracteres")
        String metodoEstudio,
        Integer aforo,
        @Size(max = 1000, message = "La instrucción admite hasta 1000 caracteres")
        String instruccion
) {}
