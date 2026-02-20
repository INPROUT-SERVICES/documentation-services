package br.com.inproutservices.documentation_service.dtos;

import java.time.LocalDate;
import java.util.Set;

public record AtualizarLancamentosDocRequest(
        Set<Long> osLpuDetalheIds,
        String documentacao,
        LocalDate planoDocumentacao,
        String situacao
) {}