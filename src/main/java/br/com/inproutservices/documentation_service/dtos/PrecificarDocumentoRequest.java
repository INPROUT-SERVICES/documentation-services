package br.com.inproutservices.documentation_service.dtos;

import java.util.Set;

public record PrecificarDocumentoRequest(
        Set<PrecificacaoItemDTO> precificacoes
) {}
