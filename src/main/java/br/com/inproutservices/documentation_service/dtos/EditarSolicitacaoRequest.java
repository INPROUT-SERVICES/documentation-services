package br.com.inproutservices.documentation_service.dtos;

import java.util.Set;

public record EditarSolicitacaoRequest(
        Long actorUsuarioId,
        String comentario,
        Long documentoId,
        Long documentistaId,
        Set<Long> lancamentoIds
) {}
