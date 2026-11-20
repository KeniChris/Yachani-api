package com.yachaniapi.ia.dto;

import com.yachaniapi.grupoestudio.entity.TipoGrupo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrupoCandidatoResponse {

    private Long idGrupo;
    private String nombre;
    private String descripcion;
    private String metodoEstudio;
    private TipoGrupo tipoGrupo;
    private String nombreCreador;
    private int integrantesActuales;
    private int capacidadMaxima;
    private int cuposDisponibles;
}