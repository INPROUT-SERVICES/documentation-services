package br.com.inproutservices.documentation_service.dtos;

import java.util.Set;

public record CriarDocumentoRequest(
        String nome,
        Set<Long> documentistaIds
) {}
