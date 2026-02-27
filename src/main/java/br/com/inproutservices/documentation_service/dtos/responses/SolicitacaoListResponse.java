package br.com.inproutservices.documentation_service.dtos.responses;

import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitacaoListResponse(
        Long id,
        Long osId,
        String os,
        String projeto,
        String osNome,
        String segmentoNome,
        StatusSolicitacaoDocumento status,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime recebidoEm,
        LocalDateTime finalizadoEm,
        DocumentoResumoResponse documento,
        Long documentistaId,
        String solicitanteNome,
        String documentistaNome,
        BigDecimal valor,
        String provaEnvio
) {}