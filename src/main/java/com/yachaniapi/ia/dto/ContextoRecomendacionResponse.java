package com.yachaniapi.ia.dto;

import com.yachaniapi.usuario.dto.ConocimientoAcademicoResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContextoRecomendacionResponse {

    // Criterios seleccionados para esta búsqueda.
    private RecomendacionGruposRequest criterios;

    // Preferencias que ya están guardadas en el perfil.
    private String modalidadPreferida;
    private String metodoPreferido;
    private List<String> temasInteres;

    // Cursos y niveles registrados por el estudiante.
    private List<ConocimientoAcademicoResponse> conocimientos;

    // Grupos disponibles que el agente podrá analizar.
    private List<GrupoCandidatoResponse> gruposCandidatos;
}