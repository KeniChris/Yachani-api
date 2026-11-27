package com.yachaniapi.ia.service;

import com.yachaniapi.grupoestudio.entity.GrupoEstudio;
import com.yachaniapi.grupoestudio.repository.GrupoEstudioRepository;
import com.yachaniapi.grupoestudio.service.ReglasGrupoService;
import com.yachaniapi.ia.dto.Response.ContextoRecomendacionResponse;
import com.yachaniapi.ia.dto.Response.GrupoCandidatoResponse;
import com.yachaniapi.ia.dto.Request.RecomendacionGruposRequest;
import com.yachaniapi.usuario.dto.PerfilAcademicoResponse;
import com.yachaniapi.usuario.entity.Estudiante;
import com.yachaniapi.usuario.service.PerfilAcademicoService;
import com.yachaniapi.usuario.service.UsuarioActualService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaCandidatosService {

    private final UsuarioActualService usuarioActual;
    private final PerfilAcademicoService perfilAcademicoService;
    private final GrupoEstudioRepository grupoRepository;
    private final ReglasGrupoService reglasGrupo;
    private final Validator validator;

    public ContextoRecomendacionResponse consultar(
            RecomendacionGruposRequest request) {

        // Obtiene la cuenta desde el JWT y exige que sea estudiante.
        Estudiante estudiante = usuarioActual.estudianteActual();

        validarSolicitud(request);

        // Consulta el perfil existente, sin modificarlo.
        PerfilAcademicoResponse perfil =
                perfilAcademicoService.consultarPerfil(
                        estudiante.getIdUsuario()
                );

        if (!perfil.isConfiguracionCompletada()) {
            throw new IllegalArgumentException(
                    "Completa tu configuración inicial antes de solicitar recomendaciones"
            );
        }

        // Copia y limpia los criterios de esta búsqueda.
        RecomendacionGruposRequest criterios =
                new RecomendacionGruposRequest();

        criterios.setCurso(request.getCurso().trim());
        criterios.setMetodoEstudio(
                limpiarOpcional(request.getMetodoEstudio())
        );
        criterios.setTipoGrupo(request.getTipoGrupo());

        // Solo consulta grupos activos.
        Specification<GrupoEstudio> activos =
                (root, query, cb) ->
                        cb.equal(root.get("estado"), "ACTIVO");

        List<GrupoCandidatoResponse> candidatos = grupoRepository
                .findAll(activos, Sort.by("idGrupo"))
                .stream()

                // Excluye grupos que creó o a los que ya pertenece.
                .filter(grupo -> !reglasGrupo.esIntegrante(
                        grupo,
                        estudiante.getIdUsuario()
                ))

                // Usa el cálculo de cupos existente.
                .filter(grupo ->
                        reglasGrupo.cuposDisponibles(grupo) > 0
                )

                // Respeta el tipo de grupo seleccionado, si lo hay.
                .filter(grupo ->
                        criterios.getTipoGrupo() == null
                                || reglasGrupo.tipoGrupo(grupo)
                                == criterios.getTipoGrupo()
                )

                // Respeta el método seleccionado, si lo hay.
                .filter(grupo ->
                        criterios.getMetodoEstudio() == null
                                || criterios.getMetodoEstudio()
                                .equalsIgnoreCase(
                                        limpiarOpcional(
                                                grupo.getMetodoEstudio()
                                        )
                                )
                )

                // Convierte las entidades en datos para el agente.
                .map(this::convertirGrupo)
                .toList();

        return new ContextoRecomendacionResponse(
                criterios,
                perfil.getModalidadPreferida(),
                perfil.getMetodoPreferido(),
                perfil.getTemasInteres() == null
                        ? List.of()
                        : List.copyOf(perfil.getTemasInteres()),
                perfil.getConocimientos() == null
                        ? List.of()
                        : List.copyOf(perfil.getConocimientos()),
                candidatos
        );
    }

    private GrupoCandidatoResponse convertirGrupo(
            GrupoEstudio grupo) {

        String nombreCreador = (
                grupo.getCreador().getNombres()
                        + " "
                        + grupo.getCreador().getApellidos()
        ).trim();

        return new GrupoCandidatoResponse(
                grupo.getIdGrupo(),
                grupo.getNombre(),
                grupo.getDescripcion(),
                grupo.getMetodoEstudio(),
                reglasGrupo.tipoGrupo(grupo),
                nombreCreador,
                reglasGrupo.integrantesActuales(grupo),
                grupo.getCapacidadMaxima(),
                reglasGrupo.cuposDisponibles(grupo)
        );
    }

    private void validarSolicitud(
            RecomendacionGruposRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Debes indicar los criterios de búsqueda"
            );
        }

        var errores = validator.validate(request);

        if (!errores.isEmpty()) {
            String mensaje = errores.stream()
                    .map(error -> error.getMessage())
                    .sorted()
                    .findFirst()
                    .orElse("Revisa los criterios de búsqueda");

            throw new IllegalArgumentException(mensaje);
        }
    }

    private String limpiarOpcional(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.trim();
    }
}