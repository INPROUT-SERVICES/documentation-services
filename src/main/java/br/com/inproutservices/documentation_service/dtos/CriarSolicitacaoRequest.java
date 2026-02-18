package br.com.inproutservices.documentation_service.dtos;

public record CriarSolicitacaoRequest(
        Long osId,
        Long documentoId,
        Long actorUsuarioId,
        String comentario
) {}