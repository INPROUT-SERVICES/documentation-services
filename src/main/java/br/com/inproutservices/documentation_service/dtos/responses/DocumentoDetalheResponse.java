package br.com.inproutservices.documentation_service.dtos.responses;

import java.util.Set;

public record DocumentoDetalheResponse(
        Long id,
        String nome,
        boolean ativo,
        Set<Long> documentistaIds,
        Set<DocumentoPrecificacaoResponse> precificacoes
) {}