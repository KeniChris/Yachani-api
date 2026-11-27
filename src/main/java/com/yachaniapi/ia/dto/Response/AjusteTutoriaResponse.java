package com.yachaniapi.ia.dto.Response;

import java.util.List;

// "cambios" solo trae los campos de la propuesta que la IA cambió; estado y faltantes se
// calculan sobre la propuesta ya con esos cambios aplicados.
public record AjusteTutoriaResponse(
        String respuesta,
        PropuestaGrupo cambios,
        EstadoPropuesta estado,
        List<FaltantePropuesta> faltantes
) {}
