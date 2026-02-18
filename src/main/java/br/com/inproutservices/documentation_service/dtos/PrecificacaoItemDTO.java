package br.com.inproutservices.documentation_service.dtos;

import java.math.BigDecimal;

public record PrecificacaoItemDTO(
        Long usuarioId,
        BigDecimal valor
) {}
