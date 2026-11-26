package com.yachaniapi.grupoestudio;

import com.yachaniapi.grupoestudio.dto.request.CrearGrupoRequest;
import com.yachaniapi.grupoestudio.dto.request.SesionInicialRequest;
import com.yachaniapi.grupoestudio.entity.GrupoEstudio;
import com.yachaniapi.grupoestudio.mapper.GrupoEstudioMapper;
import com.yachaniapi.grupoestudio.repository.GrupoEstudioRepository;
import com.yachaniapi.grupoestudio.repository.SolicitudGrupoRepository;
import com.yachaniapi.grupoestudio.service.AccesoGrupoService;
import com.yachaniapi.grupoestudio.service.GrupoEstudioService;
import com.yachaniapi.grupoestudio.service.ReglasGrupoService;
import com.yachaniapi.notificacion.service.NotificacionService;
import com.yachaniapi.recursos.dto.request.TemaRequest;
import com.yachaniapi.recursos.service.TemaGrupoService;
import com.yachaniapi.sesion.entity.SesionEstudio;
import com.yachaniapi.sesion.mapper.SesionEstudioMapper;
import com.yachaniapi.sesion.repository.SesionEstudioRepository;
import com.yachaniapi.sesion.service.NotificacionSesionService;
import com.yachaniapi.sesion.service.SesionEstudioService;
import com.yachaniapi.usuario.entity.Estudiante;
import com.yachaniapi.usuario.entity.Tutor;
import com.yachaniapi.usuario.entity.Usuario;
import com.yachaniapi.usuario.repository.UsuarioRepository;
import com.yachaniapi.usuario.service.UsuarioActualService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// Crear grupo con temas y sesiones usando los servicios reales (GrupoEstudioService y
// SesionEstudioService) detrás de los proxies @Transactional de Spring. Los repositorios están
// simulados y escriben en una "base" en memoria que solo conserva lo guardado si la transacción
// hace commit; en un rollback se descarta todo, como haría PostgreSQL.
@SpringJUnitConfig(CrearGrupoConSesionesTest.Configuracion.class)
class CrearGrupoConSesionesTest {

    // 25/11/2026 07:00 en Lima: las sesiones del 01/12 son futuras y las del 20/11 pasadas.
    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("America/Lima"));
    static final LocalDateTime FUTURA = LocalDateTime.of(2026, 12, 1, 19, 0);
    static final LocalDateTime PASADA = LocalDateTime.of(2026, 11, 20, 19, 0);

    @Configuration
    @EnableTransactionManagement
    static class Configuracion {
        @Bean BaseEnMemoria transactionManager() { return new BaseEnMemoria(); }
        @Bean Clock reloj() { return RELOJ; }
        @Bean ReglasGrupoService reglas() { return new ReglasGrupoService(); }
        @Bean GrupoEstudioMapper grupoMapper() { return Mappers.getMapper(GrupoEstudioMapper.class); }
        @Bean SesionEstudioMapper sesionMapper() { return new SesionEstudioMapper(); }
        @Bean SesionEstudioService sesionEstudioService(
                SesionEstudioRepository sesiones, GrupoEstudioRepository grupos, SesionEstudioMapper mapper,
                NotificacionSesionService notificaciones, UsuarioActualService actual,
                AccesoGrupoService acceso, Clock reloj) {
            return new SesionEstudioService(sesiones, grupos, mapper, notificaciones, actual, acceso, reloj);
        }
        @Bean GrupoEstudioService grupoEstudioService(
                GrupoEstudioRepository grupos, SolicitudGrupoRepository solicitudes, UsuarioRepository usuarios,
                NotificacionService notificaciones, GrupoEstudioMapper mapper, ReglasGrupoService reglas,
                UsuarioActualService actual, AccesoGrupoService acceso, TemaGrupoService temas,
                SesionEstudioService sesiones) {
            return new GrupoEstudioService(grupos, solicitudes, usuarios, notificaciones, mapper, reglas,
                    actual, acceso, temas, sesiones);
        }
    }

    // Gestor de transacciones que actúa como base de datos: lo escrito queda pendiente hasta el commit.
    static class BaseEnMemoria extends AbstractPlatformTransactionManager {
        final List<String> pendientes = new ArrayList<>();
        final List<String> guardados = new ArrayList<>();
        int commits;
        int rollbacks;
        // Como JpaTransactionManager: los @Transactional internos se unen a la transacción abierta.
        private boolean activa;

        void escribir(String fila) { pendientes.add(fila); }

        void reiniciar() {
            pendientes.clear();
            guardados.clear();
            commits = 0;
            rollbacks = 0;
            activa = false;
        }

        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected boolean isExistingTransaction(Object transaccion) { return activa; }
        @Override protected void doBegin(Object transaccion, TransactionDefinition definicion) { activa = true; }
        @Override protected void doCleanupAfterCompletion(Object transaccion) { activa = false; }
        @Override protected void doSetRollbackOnly(DefaultTransactionStatus estado) { }
        @Override protected void doCommit(DefaultTransactionStatus estado) {
            guardados.addAll(pendientes);
            pendientes.clear();
            commits++;
        }
        @Override protected void doRollback(DefaultTransactionStatus estado) {
            pendientes.clear();
            rollbacks++;
        }
    }

    @Autowired GrupoEstudioService service;
    @Autowired BaseEnMemoria base;

    @MockitoBean GrupoEstudioRepository grupos;
    @MockitoBean SolicitudGrupoRepository solicitudes;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean NotificacionService notificaciones;
    @MockitoBean UsuarioActualService usuarioActual;
    @MockitoBean AccesoGrupoService acceso;
    @MockitoBean TemaGrupoService temas;
    @MockitoBean SesionEstudioRepository sesionRepository;
    @MockitoBean NotificacionSesionService notificacionesSesion;

    @BeforeEach
    void preparar() {
        base.reiniciar();
        var guardado = new GrupoEstudio[1];
        when(acceso.administrador(10L, true)).thenAnswer(invocacion -> guardado[0]);
        when(grupos.save(any(GrupoEstudio.class))).thenAnswer(invocacion -> {
            GrupoEstudio grupo = invocacion.getArgument(0);
            grupo.setIdGrupo(10L);
            base.escribir("grupo:" + grupo.getNombre());
            guardado[0] = grupo;
            return grupo;
        });
        when(temas.crear(anyLong(), any(TemaRequest.class))).thenAnswer(invocacion -> {
            TemaRequest tema = invocacion.getArgument(1);
            base.escribir("tema:" + tema.nombre());
            return null;
        });
        when(sesionRepository.save(any(SesionEstudio.class))).thenAnswer(invocacion -> {
            SesionEstudio sesion = invocacion.getArgument(0);
            base.escribir("sesion:" + sesion.getTema());
            return sesion;
        });
    }

    @Test
    void unTutorCreaElGrupoConSusTemasYSesionesEnUnaSolaTransaccion() {
        iniciarSesion(tutor(1L));

        var respuesta = service.crearGrupo(solicitud(1L, List.of("Derivadas", "Integrales"), List.of(
                sesion("Repaso de derivadas", FUTURA),
                sesion("Práctica de integrales", FUTURA.plusDays(7)))));

        assertEquals(10L, respuesta.idGrupo());
        assertEquals(12, respuesta.cuposDisponibles());
        assertEquals(List.of("grupo:Dominando Cálculo", "tema:Derivadas", "tema:Integrales",
                "sesion:Repaso de derivadas", "sesion:Práctica de integrales"), base.guardados);
        assertEquals(1, base.commits);
        assertEquals(0, base.rollbacks);
    }

    @Test
    void unaSesionConFechaPasadaRevierteTodoYNoQuedaNiElGrupo() {
        iniciarSesion(tutor(1L));

        var error = assertThrows(IllegalArgumentException.class, () -> service.crearGrupo(solicitud(1L,
                List.of("Derivadas"), List.of(sesion("Repaso", FUTURA), sesion("Atrasada", PASADA)))));

        assertEquals("Programa la sesión para una fecha y hora futuras", error.getMessage());
        // Sí llegó a escribir el grupo, el tema y la primera sesión, pero el rollback lo descarta.
        verify(grupos).save(any(GrupoEstudio.class));
        assertTrue(base.guardados.isEmpty());
        assertEquals(0, base.commits);
        assertEquals(1, base.rollbacks);
    }

    @Test
    void unEstudianteNoPuedeEnviarSesionesYNoSeGuardaNada() {
        iniciarSesion(estudiante(2L));

        var error = assertThrows(IllegalArgumentException.class, () -> service.crearGrupo(
                solicitud(2L, List.of(), List.of(sesion("Repaso", FUTURA)))));

        assertEquals("Solo un tutor puede programar sesiones al crear el grupo", error.getMessage());
        verify(grupos, never()).save(any());
        verifyNoInteractions(temas, sesionRepository);
        assertTrue(base.guardados.isEmpty());
    }

    @Test
    void unGrupoSinTemasNiSesionesSeCreaIgualQueAntes() {
        iniciarSesion(estudiante(2L));

        var respuesta = service.crearGrupo(new CrearGrupoRequest(
                "Dominando Cálculo", "Ejercicios guiados", "Práctico", 12, 2L));

        assertEquals(10L, respuesta.idGrupo());
        assertEquals(List.of("grupo:Dominando Cálculo"), base.guardados);
        verifyNoInteractions(temas, sesionRepository);
    }

    @Test
    void lasSesionesInicialesUsanLasMismasValidacionesQueSesionRequest() {
        try (var fabrica = Validation.buildDefaultValidatorFactory()) {
            var validador = fabrica.getValidator();
            var corta = new SesionInicialRequest("Repaso", null, FUTURA, 10, null);
            var larga = new SesionInicialRequest("Repaso", null, FUTURA, 481, null);
            var sinTema = new SesionInicialRequest(" ", null, FUTURA, 60, null);
            var sinFecha = new SesionInicialRequest("Repaso", null, null, 60, null);
            var valida = new SesionInicialRequest("Repaso", "Ejercicios", FUTURA, 60, "Aula 3");

            assertEquals("Datos inválidos: la duración mínima es de 15 minutos",
                    validador.validate(corta).iterator().next().getMessage());
            assertEquals("Datos inválidos: la duración máxima es de 8 horas",
                    validador.validate(larga).iterator().next().getMessage());
            assertFalse(validador.validate(sinTema).isEmpty());
            assertFalse(validador.validate(sinFecha).isEmpty());
            assertTrue(validador.validate(valida).isEmpty());

            var conOnce = new CrearGrupoRequest("Grupo", "Descripción", "Práctico", 5, 1L, List.of(),
                    java.util.Collections.nCopies(11, valida));
            assertEquals("Agrega hasta 10 sesiones iniciales",
                    validador.validate(conOnce).iterator().next().getMessage());
        }
    }

    private void iniciarSesion(Usuario usuario) {
        when(usuarioActual.exigirPropio(usuario.getIdUsuario())).thenReturn(usuario);
        when(usuarioActual.idUsuario()).thenReturn(usuario.getIdUsuario());
        when(grupos.bloquearCreador(usuario.getIdUsuario())).thenReturn(Optional.of(usuario.getIdUsuario()));
    }

    private static CrearGrupoRequest solicitud(Long idCreador, List<String> temas, List<SesionInicialRequest> sesiones) {
        return new CrearGrupoRequest("Dominando Cálculo", "Ejercicios guiados de cálculo", "Práctico",
                12, idCreador, temas, sesiones);
    }

    private static SesionInicialRequest sesion(String tema, LocalDateTime fecha) {
        return new SesionInicialRequest(tema, "Resolvemos ejercicios", fecha, 90, "Aula virtual");
    }

    private static Tutor tutor(Long id) {
        Tutor tutor = new Tutor();
        tutor.setIdUsuario(id);
        tutor.setNombres("Luis");
        tutor.setApellidos("Rojas");
        tutor.setCorreo("luis@example.com");
        return tutor;
    }

    private static Estudiante estudiante(Long id) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(id);
        estudiante.setNombres("Ana");
        estudiante.setApellidos("Pérez");
        estudiante.setCorreo("ana@example.com");
        return estudiante;
    }
}
