package com.yachaniapi.ia.dto.Response;

// Dato obligatorio que falta o es inválido, con el mensaje para mostrarlo junto al campo.
public record FaltantePropuesta(String campo, String mensaje) {}
