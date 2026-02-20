package br.com.inproutservices.documentation_service.dtos;

import java.util.Set;

public record CriarSolicitacaoRequest(
        Long osId,
        Long documentoId,
        Long documentistaId,
        Long actorUsuarioId,
        String comentario,
        Set<Long> lancamentoIds
) {}