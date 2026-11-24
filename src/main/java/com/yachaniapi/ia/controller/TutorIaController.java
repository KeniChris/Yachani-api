package com.yachaniapi.ia.controller;

import com.yachaniapi.ia.dto.Request.AsistenteGrupoRequest;
import com.yachaniapi.ia.dto.Request.BorradorGrupoRequest;
import com.yachaniapi.ia.dto.Response.AsistenteGrupoResponse;
import com.yachaniapi.ia.dto.Response.BorradorGrupoResponse;
import com.yachaniapi.ia.dto.Response.TranscripcionResponse;
import com.yachaniapi.ia.service.AsistenteGrupoService;
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

@RestController
@RequestMapping("/ia")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR')")
public class TutorIaController {
    private final BorradorGrupoService borradores;
    private final TranscripcionIaService transcripciones;
    private final AsistenteGrupoService asistente;

    // Chat del asistente IA en "Crear grupo": responde y propone cambios al formulario.
    @PostMapping("/tutorias/asistente")
    public AsistenteGrupoResponse conversar(@Valid @RequestBody AsistenteGrupoRequest request) {
        return asistente.responder(request);
    }

    @PostMapping("/tutorias/borrador")
    public BorradorGrupoResponse generarBorrador(@Valid @RequestBody BorradorGrupoRequest request) {
        return borradores.generar(request.instruccion());
    }

    @PostMapping(value = "/voz/transcribir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscripcionResponse transcribir(@RequestParam("audio") MultipartFile audio) {
        return new TranscripcionResponse(transcripciones.transcribir(audio));
    }
}
