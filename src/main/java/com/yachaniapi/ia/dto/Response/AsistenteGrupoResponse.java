package com.yachaniapi.ia.dto.Response;

public record AsistenteGrupoResponse(
        String respuesta,
        Sugerencias sugerencias
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
}