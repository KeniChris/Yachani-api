package com.yachaniapi.ia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteIa {
    private final ObjectProvider<ChatClient.Builder> clientes;

    public <T> T generar(String instrucciones, String entrada, Class<T> tipo) {
        ChatClient.Builder builder = clientes.getIfAvailable();
        if (builder == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "La IA no está configurada");
        }

        try {
            T resultado = builder.build()
                    .prompt()
                    .system(instrucciones)
                    .messages(new UserMessage(entrada))
                    .call()
                    .entity(tipo, spec -> spec.useProviderStructuredOutput());

            if (resultado != null) {
                return resultado;
            }
        } catch (RuntimeException error) {
            log.warn("Falló la llamada al modelo de IA ({})",
                    error.getClass().getSimpleName());
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "La IA no está disponible en este momento");
    }
}