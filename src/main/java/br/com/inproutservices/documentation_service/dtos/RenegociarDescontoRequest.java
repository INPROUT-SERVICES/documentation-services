package br.com.inproutservices.documentation_service.dtos;

import java.math.BigDecimal;

public record RenegociarDescontoRequest(
        Long actorUsuarioId,
        BigDecimal novoPercentualDesconto,
        String comentario
) {}
