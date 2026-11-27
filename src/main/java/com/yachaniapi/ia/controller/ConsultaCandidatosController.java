package com.yachaniapi.ia.controller;

import com.yachaniapi.ia.dto.Response.ContextoRecomendacionResponse;
import com.yachaniapi.ia.dto.Request.RecomendacionGruposRequest;
import com.yachaniapi.ia.dto.Response.RecomendacionGruposResponse;
import com.yachaniapi.ia.service.ConsultaCandidatosService;
import com.yachaniapi.ia.service.RecomendacionGruposService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ia/grupos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class ConsultaCandidatosController {
    private final ConsultaCandidatosService consultaCandidatosService;
    private final RecomendacionGruposService recomendaciones;

    @PostMapping("/candidatos")
    public ContextoRecomendacionResponse consultar(
            @Valid @RequestBody RecomendacionGruposRequest request) {
        return consultaCandidatosService.consultar(request);
    }

    @PostMapping("/recomendar")
    public RecomendacionGruposResponse recomendar(
            @Valid @RequestBody RecomendacionGruposRequest request) {
        return recomendaciones.recomendar(request);
    }
}