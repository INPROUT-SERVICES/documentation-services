package br.com.inproutservices.documentation_service.dtos.responses;

import java.util.Set;

public record DocumentoListResponse(
        Long id,
        String nome,
        boolean ativo,
        Set<Long> documentistaIds
) {}