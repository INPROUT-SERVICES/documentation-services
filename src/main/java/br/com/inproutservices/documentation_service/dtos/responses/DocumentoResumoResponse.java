package br.com.inproutservices.documentation_service.dtos.responses;

public record DocumentoResumoResponse(
        Long id,
        String nome,
        boolean ativo
) {}

