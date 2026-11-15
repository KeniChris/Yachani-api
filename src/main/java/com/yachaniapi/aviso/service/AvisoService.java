package com.yachaniapi.aviso.service;

import com.yachaniapi.aviso.dto.AvisoDTO;
import com.yachaniapi.aviso.entity.Aviso;
import com.yachaniapi.aviso.exception.AvisoExceptions.AutorNoAutorizadoException;
import com.yachaniapi.aviso.exception.AvisoExceptions.AvisoNoEncontradoException;
import com.yachaniapi.aviso.mapper.AvisoMapper;
import com.yachaniapi.aviso.repository.AvisoRepository;
import com.yachaniapi.grupoestudio.service.AccesoGrupoService;
import com.yachaniapi.notificacion.service.NotificacionService;
import com.yachaniapi.usuario.entity.Tutor;
import com.yachaniapi.usuario.service.UsuarioActualService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvisoService {

    private final AvisoRepository avisoRepository;
    private final AvisoMapper avisoMapper;
    private final UsuarioActualService usuarioActual;
    private final AccesoGrupoService acceso;
    private final NotificacionService notificacionService;

    @Transactional
    public AvisoDTO.Response publicarAviso(Long idGrupo, AvisoDTO.CrearRequest request) {
        Tutor autor = exigirTutor(request.getIdAutor());
        var grupo = acceso.administrador(idGrupo, true);

        Aviso aviso = new Aviso();
        aviso.setGrupo(grupo);
        aviso.setAutor(autor);
        aviso.setTitulo(request.getTitulo().trim());
        aviso.setMensaje(request.getMensaje().trim());

        Aviso publicado = avisoRepository.save(aviso);
        notificacionService.notificarAvisoPublicado(publicado);
        return avisoMapper.toResponse(publicado);
    }

    @Transactional(readOnly = true)
    public List<AvisoDTO.Response> listarAvisos(Long idGrupo) {
        acceso.miembro(idGrupo, false);
        return avisoRepository.findByGrupo_IdGrupoOrderByFechaPublicacionDesc(idGrupo)
                .stream()
                .map(avisoMapper::toResponse)
                .toList();
    }

    @Transactional
    public AvisoDTO.Response editarAviso(Long idGrupo, Long idAviso, AvisoDTO.EditarRequest request) {
        exigirTutor(request.getIdAutor());
        acceso.administrador(idGrupo, true);

        Aviso aviso = avisoRepository.findByIdAvisoAndGrupo_IdGrupo(idAviso, idGrupo)
                .orElseThrow(() -> new AvisoNoEncontradoException("El aviso no existe"));

        aviso.setTitulo(request.getTitulo().trim());
        aviso.setMensaje(request.getMensaje().trim());
        aviso.setFechaPublicacion(LocalDateTime.now());

        return avisoMapper.toResponse(avisoRepository.save(aviso));
    }

    private Tutor exigirTutor(Long idAutor) {
        if (Hibernate.unproxy(usuarioActual.exigirPropio(idAutor)) instanceof Tutor tutor) {
            return tutor;
        }
        throw new AutorNoAutorizadoException("Solo el tutor del grupo puede publicar o editar avisos");
    }
}