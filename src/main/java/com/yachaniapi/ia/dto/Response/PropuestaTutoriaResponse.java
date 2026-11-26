package com.yachaniapi.ia.dto.Response;

import java.util.List;

// Con estado INCOMPLETA, "propuesta" llega en null y "faltantes" dice qué completar.
public record PropuestaTutoriaResponse(
        EstadoPropuesta estado,
        List<FaltantePropuesta> faltantes,
        PropuestaGrupo propuesta
) {}
