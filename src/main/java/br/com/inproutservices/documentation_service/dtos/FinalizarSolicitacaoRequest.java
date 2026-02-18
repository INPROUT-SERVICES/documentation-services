package br.com.inproutservices.documentation_service.dtos;

public record FinalizarSolicitacaoRequest(
        Long actorUsuarioId,
        String comentario,
        String provaEnvio
) {}
