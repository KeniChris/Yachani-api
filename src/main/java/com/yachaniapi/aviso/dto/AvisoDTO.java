package com.yachaniapi.aviso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class AvisoDTO {
    private static final String CAMPOS_INVALIDOS = "Llene correctamente los campos requeridos";

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CrearRequest {
        private Long idAutor;

        @NotBlank(message = CAMPOS_INVALIDOS)
        @Size(max = 100, message = CAMPOS_INVALIDOS)
        private String titulo;

        @NotBlank(message = CAMPOS_INVALIDOS)
        @Size(max = 500, message = CAMPOS_INVALIDOS)
        private String mensaje;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EditarRequest {
        private Long idAutor;

        @NotBlank(message = CAMPOS_INVALIDOS)
        @Size(max = 100, message = CAMPOS_INVALIDOS)
        private String titulo;

        @NotBlank(message = CAMPOS_INVALIDOS)
        @Size(max = 500, message = CAMPOS_INVALIDOS)
        private String mensaje;
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long idAviso;
        private Long idGrupo;
        private Long idAutor;
        private String nombreAutor;
        private String titulo;
        private String mensaje;
        private LocalDateTime fechaPublicacion;
    }
}
