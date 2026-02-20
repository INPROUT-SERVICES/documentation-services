package br.com.inproutservices.documentation_service.dtos.responses;

import br.com.inproutservices.documentation_service.dtos.UsuarioDTO;
import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitacaoDetalheResponse(
        Long id,
        Long osId,
        StatusSolicitacaoDocumento status,
        boolean ativo,
        String provaEnvio,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime recebidoEm,
        LocalDateTime finalizadoEm,
        DocumentoResumoResponse documento,
        Long documentistaId,
        UsuarioDTO documentista,
        BigDecimal valorDoDocumentistaNaSolicitacao
) {}
