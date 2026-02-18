package br.com.inproutservices.documentation_service.mappers;

import br.com.inproutservices.documentation_service.dtos.responses.DocumentoDetalheResponse;
import br.com.inproutservices.documentation_service.dtos.responses.DocumentoListResponse;
import br.com.inproutservices.documentation_service.dtos.responses.DocumentoPrecificacaoResponse;
import br.com.inproutservices.documentation_service.dtos.responses.DocumentoResumoResponse;
import br.com.inproutservices.documentation_service.entities.Documento;
import br.com.inproutservices.documentation_service.entities.DocumentoPrecificacao;

import java.util.Set;
import java.util.stream.Collectors;

public final class DocumentoMapper {

    private DocumentoMapper() {
    }

    public static DocumentoResumoResponse toResumo(Documento doc) {
        if (doc == null) return null;

        return new DocumentoResumoResponse(
                doc.getId(),
                doc.getNome(),
                doc.isAtivo()
        );
    }

    public static DocumentoListResponse toList(Documento doc) {
        if (doc == null) return null;

        return new DocumentoListResponse(
                doc.getId(),
                doc.getNome(),
                doc.isAtivo(),
                doc.getDocumentistasIds()
        );
    }

    public static DocumentoDetalheResponse toDetalhe(Documento doc) {
        if (doc == null) return null;

        Set<DocumentoPrecificacaoResponse> precs = doc.getPrecificacoes() == null
                ? Set.of()
                : doc.getPrecificacoes().stream()
                .map(DocumentoMapper::toPrecificacao)
                .collect(Collectors.toSet());

        return new DocumentoDetalheResponse(
                doc.getId(),
                doc.getNome(),
                doc.isAtivo(),
                doc.getDocumentistasIds(),
                precs
        );
    }

    public static DocumentoPrecificacaoResponse toPrecificacao(DocumentoPrecificacao p) {
        if (p == null) return null;

        return new DocumentoPrecificacaoResponse(
                p.getUsuarioId(),
                p.getValor()
        );
    }
}