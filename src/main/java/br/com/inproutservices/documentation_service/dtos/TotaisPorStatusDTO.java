package br.com.inproutservices.documentation_service.dtos;

import java.math.BigDecimal;

public record TotaisPorStatusDTO(
        BigDecimal aguardandoRecebimento,
        BigDecimal recebido,
        BigDecimal finalizado
) {}
