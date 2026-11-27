package com.yachaniapi.ia.dto.Response;

import java.util.List;

public record AsistenteGrupoResponse(
        String respuesta,
        Sugerencias sugerencias,
        List<String> temas,
        List<SesionSugerida> planSesiones,
        List<String> faltantes
) {
    public record Sugerencias(
            String nombre,
            String descripcion,
            String metodoEstudio,
            Integer capacidadMaxima
    ) {
        public static Sugerencias sinCambios() {
            return new Sugerencias(null, null, null, null);
        }
    }

    public record SesionSugerida(
            int numero,
            String tema,
            String objetivo
    ) {}
}
