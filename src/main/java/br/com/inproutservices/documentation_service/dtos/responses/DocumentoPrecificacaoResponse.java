package br.com.inproutservices.documentation_service.dtos.responses;

import java.math.BigDecimal;

public record DocumentoPrecificacaoResponse(
        Long usuarioId,
        BigDecimal valor
) {}