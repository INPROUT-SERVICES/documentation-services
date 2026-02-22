package br.com.inproutservices.documentation_service.dtos.responses;

import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import br.com.inproutservices.documentation_service.enums.TipoEventoSolicitacao;

import java.time.LocalDateTime;

public record SolicitacaoEventoResponse(
        Long id,
        TipoEventoSolicitacao tipoEvento,
        StatusSolicitacaoDocumento statusAnterior,
        StatusSolicitacaoDocumento statusNovo,
        String comentario,
        Long actorUsuarioId,
        String actorNome,
        LocalDateTime criadoEm
) {}