package br.com.inproutservices.documentation_service.services;

import br.com.inproutservices.documentation_service.dtos.AtualizarDocumentoRequest;
import br.com.inproutservices.documentation_service.dtos.CriarDocumentoRequest;
import br.com.inproutservices.documentation_service.dtos.PrecificacaoItemDTO;
import br.com.inproutservices.documentation_service.dtos.PrecificarDocumentoRequest;
import br.com.inproutservices.documentation_service.entities.Documento;
import br.com.inproutservices.documentation_service.entities.DocumentoPrecificacao;
import br.com.inproutservices.documentation_service.repositories.DocumentoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;

    // =========================
    // DOCUMENTO
    // =========================

    @Transactional
    public Documento criarDocumento(CriarDocumentoRequest request) {
        validarCriarOuAlterar(request);

        Documento doc = new Documento();
        doc.setNome(request.nome().trim());
        doc.setAtivo(true);

        Set<Long> ids = request.documentistaIds() == null ? new HashSet<>() : new HashSet<>(request.documentistaIds());
        validarIdsDocumentistas(ids);

        doc.setDocumentistasIds(ids);

        return documentoRepository.save(doc);
    }

    @Transactional
    public Documento alterarDocumento(Long documentoId, AtualizarDocumentoRequest request) {
        validarCriarOuAlterar(request);

        Documento doc = buscarDocumentoOuFalhar(documentoId);

        doc.setNome(request.nome().trim());

        Set<Long> ids = request.documentistaIds() == null ? new HashSet<>() : new HashSet<>(request.documentistaIds());
        validarIdsDocumentistas(ids);

        doc.getDocumentistasIds().clear();
        doc.getDocumentistasIds().addAll(ids);

        //se remover documentistas, limpa precificações de usuários que não estão mais na lista
        doc.getPrecificacoes().removeIf(p -> !doc.getDocumentistasIds().contains(p.getUsuarioId()));

        return documentoRepository.save(doc);
    }

    @Transactional
    public void desativarDocumento(Long documentoId) {
        Documento doc = buscarDocumentoOuFalhar(documentoId);
        doc.setAtivo(false);
        documentoRepository.save(doc);
    }

    @Transactional
    public void ativarDocumento(Long documentoId) {
        Documento doc = buscarDocumentoOuFalhar(documentoId);
        doc.setAtivo(true);
        documentoRepository.save(doc);
    }

    public Documento buscarDocumento(Long documentoId) {
        return buscarDocumentoOuFalhar(documentoId);
    }

    public List<Documento> listarTodosDocumentos() {
        return documentoRepository.findAll();
    }

    public List<Documento> listarDocumentosAtivos() {
        return documentoRepository.findAll()
                .stream()
                .filter(Documento::isAtivo)
                .toList();
    }

    @Transactional
    public Documento precificarDocumento(Long documentoId, PrecificarDocumentoRequest request) {
        Documento doc = buscarDocumentoOuFalhar(documentoId);

        if (!doc.isAtivo()) {
            throw new RuntimeException("Documento desativado não pode ser precificado.");
        }

        if (doc.getDocumentistasIds() == null || doc.getDocumentistasIds().isEmpty()) {
            throw new RuntimeException("Documento não possui documentistas para precificar.");
        }

        if (request == null || request.precificacoes() == null || request.precificacoes().isEmpty()) {
            throw new RuntimeException("Informe as precificações.");
        }

        validarPrecificacoes(doc, request.precificacoes());

        doc.getPrecificacoes().clear();

        for (PrecificacaoItemDTO item : request.precificacoes()) {
            DocumentoPrecificacao p = new DocumentoPrecificacao();
            p.setUsuarioId(item.usuarioId());
            p.setValor(item.valor());
            p.setDocumento(doc);

            doc.getPrecificacoes().add(p);
        }

        return documentoRepository.save(doc);
    }

    public BigDecimal buscarValorDoDocumentistaNoDocumento(Long documentoId, Long usuarioId) {
        Documento doc = buscarDocumentoOuFalhar(documentoId);

        if (usuarioId == null) {
            throw new RuntimeException("usuarioId é obrigatório.");
        }

        return doc.getPrecificacoes().stream()
                .filter(p -> Objects.equals(p.getUsuarioId(), usuarioId))
                .map(DocumentoPrecificacao::getValor)
                .findFirst()
                .orElse(null);
    }

    // =========================
    // HELPERS
    // =========================

    private Documento buscarDocumentoOuFalhar(Long documentoId) {
        if (documentoId == null) {
            throw new RuntimeException("documentoId é obrigatório.");
        }

        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado."));
    }

    private void validarCriarOuAlterar(CriarDocumentoRequest request) {
        if (request == null) throw new RuntimeException("Payload inválido.");
        validarNome(request.nome());
    }

    private void validarCriarOuAlterar(AtualizarDocumentoRequest request) {
        if (request == null) throw new RuntimeException("Payload inválido.");
        validarNome(request.nome());
    }

    private void validarNome(String nome) {
        if (nome == null || nome.trim().length() < 3) {
            throw new RuntimeException("Nome do documento é obrigatório (mínimo 3 caracteres).");
        }
    }

    private void validarIdsDocumentistas(Set<Long> ids) {
        if (ids == null) return;

        if (ids.contains(null)) {
            throw new RuntimeException("Lista de documentistas contém usuário inválido (null).");
        }
        if (ids.size() > 0 && ids.stream().anyMatch(id -> id <= 0)) {
            throw new RuntimeException("Lista de documentistas contém usuário inválido (<= 0).");
        }
    }

    private void validarPrecificacoes(Documento doc, Set<PrecificacaoItemDTO> precificacoes) {
        Set<Long> repetidos = new HashSet<>();
        Set<Long> vistos = new HashSet<>();

        for (PrecificacaoItemDTO item : precificacoes) {
            if (item == null) throw new RuntimeException("Item de precificação inválido.");

            Long usuarioId = item.usuarioId();
            BigDecimal valor = item.valor();

            if (usuarioId == null) throw new RuntimeException("usuarioId é obrigatório.");
            if (!doc.getDocumentistasIds().contains(usuarioId)) {
                throw new RuntimeException("Usuário " + usuarioId + " não é documentista deste documento.");
            }

            if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Valor inválido para usuário " + usuarioId + ".");
            }

            if (!vistos.add(usuarioId)) {
                repetidos.add(usuarioId);
            }
        }

        if (!repetidos.isEmpty()) {
            throw new RuntimeException("Existem usuários repetidos na precificação: " + repetidos);
        }
    }
}
