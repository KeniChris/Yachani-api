package com.yachaniapi.ia.dto;

import com.yachaniapi.grupoestudio.entity.TipoGrupo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecomendacionGruposRequest {

    @NotBlank(message = "Ingresa un curso o tema")
    @Size(
            max = 120,
            message = "El curso o tema no debe superar los 120 caracteres"
    )
    private String curso;

    @Size(
            max = 50,
            message = "El método no debe superar los 50 caracteres"
    )
    private String metodoEstudio;

    private TipoGrupo tipoGrupo;
}