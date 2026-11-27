package com.yachaniapi.ia.controller;

import com.yachaniapi.ia.dto.Request.BorradorGrupoRequest;
import com.yachaniapi.ia.dto.Response.BorradorGrupoResponse;
import com.yachaniapi.ia.dto.Response.TranscripcionResponse;
import com.yachaniapi.ia.service.BorradorGrupoService;
import com.yachaniapi.ia.service.TranscripcionIaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest;
import com.yachaniapi.ia.dto.Response.AsistenteGrupoResponse;
import com.yachaniapi.ia.service.AsistenteGrupoService;
import com.yachaniapi.ia.dto.Request.AjusteTutoriaRequest;
import com.yachaniapi.ia.dto.Request.PropuestaTutoriaRequest;
import com.yachaniapi.ia.dto.Response.AjusteTutoriaResponse;
import com.yachaniapi.ia.dto.Response.PropuestaTutoriaResponse;
import com.yachaniapi.ia.service.PropuestaTutoriaService;

@RestController
@RequestMapping("/ia")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR')")
public class TutorIaController {
    private final BorradorGrupoService borradores;
    private final TranscripcionIaService transcripciones;
    private final AsistenteGrupoService asistente;
    private final PropuestaTutoriaService propuestas;

    @PostMapping("/tutorias/borrador")
    public BorradorGrupoResponse generarBorrador(@Valid @RequestBody BorradorGrupoRequest request) {
        return borradores.generar(request.instruccion());
    }

    @PostMapping("/tutorias/asistente")
    public AsistenteGrupoResponse conversarConAsistente(@Valid @RequestBody AsistenteGrupoRequest request) {
        return asistente.conversar(request);
    }

    // US22 · Agente de propuesta: arma la propuesta completa (o dice qué falta, sin llamar a la IA).
    @PostMapping("/tutorias/propuesta")
    public PropuestaTutoriaResponse generarPropuesta(@Valid @RequestBody PropuestaTutoriaRequest request) {
        return propuestas.generar(request);
    }

    // US22 · Agente de propuesta: aplica un ajuste pedido en el chat y devuelve solo lo que cambia.
    @PostMapping("/tutorias/ajuste")
    public AjusteTutoriaResponse ajustarPropuesta(@Valid @RequestBody AjusteTutoriaRequest request) {
        return propuestas.ajustar(request);
    }

    @PostMapping(value = "/voz/transcribir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscripcionResponse transcribir(@RequestParam("audio") MultipartFile audio) {
        return new TranscripcionResponse(transcripciones.transcribir(audio));
    }
}
