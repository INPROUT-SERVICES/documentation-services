package br.com.inproutservices.documentation_service.mappers;

import br.com.inproutservices.documentation_service.dtos.UsuarioDTO;
import br.com.inproutservices.documentation_service.dtos.responses.DocumentoResumoResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoDetalheResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoEventoResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoListResponse;
import br.com.inproutservices.documentation_service.entities.DocumentoPrecificacao;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumentoEvento;

import java.math.BigDecimal;
import java.util.Objects;

public final class SolicitacaoMapper {

    private SolicitacaoMapper() {
    }

    // =========================
    // LIST RESPONSE (tabelas)
    // =========================
    public static SolicitacaoListResponse toList(SolicitacaoDocumento s) {
        if (s == null) return null;

        return new SolicitacaoListResponse(
                s.getId(),
                s.getOsId(),
                s.getStatus(),
                s.isAtivo(),
                s.getCriadoEm(),
                s.getAtualizadoEm(),
                toDocumentoResumo(s),
                s.getDocumentistaId()
        );
    }

    // =========================
    // DETALHE (com documentista opcional)
    // =========================
    public static SolicitacaoDetalheResponse toDetalhe(SolicitacaoDocumento s,
                                                       UsuarioDTO documentista,
                                                       BigDecimal valorDoDocumentista) {
        if (s == null) return null;

        return new SolicitacaoDetalheResponse(
                s.getId(),
                s.getOsId(),
                s.getStatus(),
                s.isAtivo(),
                s.getProvaEnvio(),
                s.getCriadoEm(),
                s.getAtualizadoEm(),
                toDocumentoResumo(s),
                s.getDocumentistaId(),
                documentista,
                valorDoDocumentista
        );
    }

    // =========================
    // EVENTO / HISTÓRICO
    // =========================
    public static SolicitacaoEventoResponse toEvento(SolicitacaoDocumentoEvento e) {
        if (e == null) return null;

        return new SolicitacaoEventoResponse(
                e.getId(),
                e.getTipoEvento(),
                e.getStatusAnterior(),
                e.getStatusNovo(),
                e.getComentario(),
                e.getActorUsuarioId(),
                e.getCriadoEm()
        );
    }

    // =========================
    // VALOR DO DOCUMENTISTA NO DOCUMENTO
    // =========================
    public static BigDecimal valorDoDocumentistaNoDocumento(SolicitacaoDocumento s, Long usuarioId) {
        if (s == null || s.getDocumento() == null || s.getDocumento().getPrecificacoes() == null) return null;

        return s.getDocumento().getPrecificacoes().stream()
                .filter(p -> Objects.equals(p.getUsuarioId(), usuarioId))
                .map(DocumentoPrecificacao::getValor)
                .findFirst()
                .orElse(null);
    }

    // =========================
    // DOCUMENTO RESUMO
    // =========================
    private static DocumentoResumoResponse toDocumentoResumo(SolicitacaoDocumento s) {
        if (s.getDocumento() == null) return null;

        return new DocumentoResumoResponse(
                s.getDocumento().getId(),
                s.getDocumento().getNome(),
                s.getDocumento().isAtivo()
        );
    }
}