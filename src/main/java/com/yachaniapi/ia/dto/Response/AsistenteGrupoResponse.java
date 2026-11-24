package com.yachaniapi.ia.dto.Response;

// Respuesta del asistente: el texto para el chat y los cambios propuestos al formulario.
// En sugerencias solo vienen los campos que la IA propone cambiar; el resto llega en null.
public record AsistenteGrupoResponse(String respuesta, Sugerencias sugerencias) {

    public record Sugerencias(
            String nombre,
            String descripcion,
            String metodoEstudio,
            Integer capacidadMaxima
    ) {
        public boolean vacias() {
            return nombre == null && descripcion == null && metodoEstudio == null && capacidadMaxima == null;
        }
    }
}
