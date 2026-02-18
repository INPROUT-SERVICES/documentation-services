package br.com.inproutservices.documentation_service.dtos;

public record AcaoSolicitacaoRequest(
        Long actorUsuarioId,
        String comentario
) {}
