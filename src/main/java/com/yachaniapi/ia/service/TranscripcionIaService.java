package com.yachaniapi.ia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranscripcionIaService {
    private final ObjectProvider<TranscriptionModel> modelos;

    public String transcribir(MultipartFile audio) {
        if (audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El audio está vacío");
        }
        TranscriptionModel modelo = modelos.getIfAvailable();
        if (modelo == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La transcripción no está configurada");
        }

        String texto;
        try {
            texto = modelo.transcribe(audio.getResource());
        } catch (RuntimeException error) {
            log.warn("Falló la transcripción ({})", error.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo transcribir el audio");
        }
        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se detectó texto en el audio");
        }
        return texto.strip();
    }
}
