package com.yachaniapi.ia.dto.Response;

import com.yachaniapi.grupoestudio.entity.TipoGrupo;

import java.util.List;

public record RecomendacionGruposResponse(List<Grupo> recomendaciones) {
    public record Grupo(
            Long idGrupo,
            String nombre,
            String descripcion,
            String metodoEstudio,
            TipoGrupo tipoGrupo,
            int cuposDisponibles,
            String motivo
    ) {}
}