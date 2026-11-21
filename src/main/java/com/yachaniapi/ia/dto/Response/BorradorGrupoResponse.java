package com.yachaniapi.ia.dto.Response;

import java.util.List;

public record BorradorGrupoResponse(
        String nombre,
        String descripcion,
        String metodoEstudio,
        Integer capacidadMaxima,
        List<String> temasSugeridos
) {}
