package com.yachaniapi.ia.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BorradorGrupoRequest(
        @NotBlank(message = "Escribe o dicta una instrucción para el grupo")
        @Size(max = 2000, message = "La instrucción admite hasta 2000 caracteres")
        String instruccion
) {}
