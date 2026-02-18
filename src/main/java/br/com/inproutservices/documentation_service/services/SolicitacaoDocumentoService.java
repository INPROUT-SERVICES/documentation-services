package br.com.inproutservices.documentation_service.services;

import br.com.inproutservices.documentation_service.dtos.AcaoSolicitacaoRequest;
import br.com.inproutservices.documentation_service.dtos.FinalizarSolicitacaoRequest;
import br.com.inproutservices.documentation_service.dtos.TotaisPorStatusDTO;
import br.com.inproutservices.documentation_service.entities.Documento;
import br.com.inproutservices.documentation_service.entities.DocumentoPrecificacao;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumentoEvento;
import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import br.com.inproutservices.documentation_service.enums.TipoEventoSolicitacao;
import br.com.inproutservices.documentation_service.repositories.DocumentoRepository;
import br.com.inproutservices.documentation_service.repositories.SolicitacaoDocumentoEventoRepository;
import br.com.inproutservices.documentation_service.repositories.SolicitacaoDocumentoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SolicitacaoDocumentoService {

    private final SolicitacaoDocumentoRepository solicitacaoRepository;
    private final SolicitacaoDocumentoEventoRepository eventoRepository;
    private final DocumentoRepository documentoRepository;

    // =========================
    // SOLICITAÇÃO
    // =========================

    @Transactional
    public SolicitacaoDocumento criarSolicitacao(Long osId, Long documentoId, Long actorUsuarioId, String comentario) {
        validarComentario(comentario);

        if (osId == null || osId <= 0) throw new RuntimeException("osId é obrigatório.");
        if (documentoId == null || documentoId <= 0) throw new RuntimeException("documentoId é obrigatório.");

        // IMPORTANTE: isso depende do repository corrigido (veja ajuste logo abaixo)
        if (solicitacaoRepository.existsByOsIdAndDocumento_Id(osId, documentoId)) {
            throw new RuntimeException("Já existe solicitação deste documento para esta OS.");
        }

        Documento doc = buscarDocumentoOuFalhar(documentoId);

        if (!doc.isAtivo()) {
            throw new RuntimeException("Documento desativado não pode ser solicitado.");
        }

        SolicitacaoDocumento solicitacao = new SolicitacaoDocumento();
        solicitacao.setOsId(osId);
        solicitacao.setDocumento(doc);
        solicitacao.setAtivo(true);
        solicitacao.setStatus(StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        solicitacao.setProvaEnvio(null);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(solicitacao);

        registrarEvento(salvo, TipoEventoSolicitacao.CRIADA, null, salvo.getStatus(), comentario, actorUsuarioId);

        return salvo;
    }

    public SolicitacaoDocumento buscarSolicitacao(Long solicitacaoId) {
        if (solicitacaoId == null || solicitacaoId <= 0) {
            throw new RuntimeException("solicitacaoId é obrigatório.");
        }

        return solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada."));
    }

    // =========================
    // FLUXO
    // =========================

    @Transactional
    public SolicitacaoDocumento marcarRecebido(Long solicitacaoId, AcaoSolicitacaoRequest request) {
        validarAcao(request);

        SolicitacaoDocumento s = buscarSolicitacaoOuFalhar(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO) {
            throw new RuntimeException("Solicitação não está aguardando recebimento.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();
        s.setStatus(StatusSolicitacaoDocumento.RECEBIDO);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.MARCADO_RECEBIDO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        return salvo;
    }

    @Transactional
    public SolicitacaoDocumento finalizar(Long solicitacaoId, FinalizarSolicitacaoRequest request) {
        validarFinalizacao(request);

        SolicitacaoDocumento s = buscarSolicitacaoOuFalhar(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.RECEBIDO) {
            throw new RuntimeException("Solicitação precisa estar RECEBIDO para finalizar.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();
        s.setProvaEnvio(request.provaEnvio().trim());
        s.setStatus(StatusSolicitacaoDocumento.FINALIZADO);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.FINALIZADO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        return salvo;
    }

    @Transactional
    public SolicitacaoDocumento recusar(Long solicitacaoId, AcaoSolicitacaoRequest request) {
        validarAcao(request);

        SolicitacaoDocumento s = buscarSolicitacaoOuFalhar(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.RECEBIDO) {
            throw new RuntimeException("Solicitação precisa estar RECEBIDO para recusar.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();

        s.setStatus(StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        s.setProvaEnvio(null);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.RECUSADO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        return salvo;
    }

    @Transactional
    public void comentar(Long solicitacaoId, AcaoSolicitacaoRequest request) {
        validarAcao(request);

        SolicitacaoDocumento s = buscarSolicitacaoOuFalhar(solicitacaoId);

        registrarEvento(s, TipoEventoSolicitacao.COMENTARIO, s.getStatus(), s.getStatus(),
                request.comentario(), request.actorUsuarioId());
    }

    // =========================
    // LISTAGENS
    // =========================

    public List<SolicitacaoDocumento> listarPorOs(Long osId) {
        if (osId == null || osId <= 0) throw new RuntimeException("osId é obrigatório.");
        return solicitacaoRepository.findByOsId(osId);
    }

    public List<SolicitacaoDocumento> listarPorStatus(StatusSolicitacaoDocumento status) {
        if (status == null) throw new RuntimeException("status é obrigatório.");
        return solicitacaoRepository.findByStatus(status);
    }

    public List<SolicitacaoDocumento> listarPorOsEStatus(Long osId, StatusSolicitacaoDocumento status) {
        if (osId == null || osId <= 0) throw new RuntimeException("osId é obrigatório.");
        if (status == null) throw new RuntimeException("status é obrigatório.");
        return solicitacaoRepository.findByOsIdAndStatus(osId, status);
    }

    public List<SolicitacaoDocumento> listarSolicitacoesDoDocumentista(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) throw new RuntimeException("usuarioId é obrigatório.");
        return solicitacaoRepository.listarPorDocumentista(usuarioId);
    }

    public List<SolicitacaoDocumento> listarSolicitacoesDoDocumentistaPorStatus(Long usuarioId, StatusSolicitacaoDocumento status) {
        if (usuarioId == null || usuarioId <= 0) throw new RuntimeException("usuarioId é obrigatório.");
        if (status == null) throw new RuntimeException("status é obrigatório.");
        return solicitacaoRepository.listarPorDocumentistaEStatus(usuarioId, status);
    }

    public List<SolicitacaoDocumentoEvento> historico(Long solicitacaoId) {
        if (solicitacaoId == null || solicitacaoId <= 0) throw new RuntimeException("solicitacaoId é obrigatório.");
        return eventoRepository.findBySolicitacaoIdOrderByCriadoEmAsc(solicitacaoId);
    }

    // =========================
    // TOTAIS
    // =========================

    public BigDecimal totalDoDocumentistaPorStatus(Long usuarioId, StatusSolicitacaoDocumento status) {
        if (usuarioId == null || usuarioId <= 0) throw new RuntimeException("usuarioId é obrigatório.");
        if (status == null) throw new RuntimeException("status é obrigatório.");

        List<SolicitacaoDocumento> solicitacoes = solicitacaoRepository.listarPorDocumentistaEStatus(usuarioId, status);

        BigDecimal total = BigDecimal.ZERO;

        for (SolicitacaoDocumento s : solicitacoes) {
            BigDecimal valor = buscarValorDoDocumentistaNoDocumento(s, usuarioId);
            if (valor != null) {
                total = total.add(valor);
            }
        }

        return total;
    }

    public TotaisPorStatusDTO totaisDoDocumentistaPorStatus(Long usuarioId) {
        BigDecimal aguardando = totalDoDocumentistaPorStatus(usuarioId, StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        BigDecimal recebido = totalDoDocumentistaPorStatus(usuarioId, StatusSolicitacaoDocumento.RECEBIDO);

        BigDecimal finalizado = totalDoDocumentistaPorStatus(usuarioId, StatusSolicitacaoDocumento.FINALIZADO)
                .add(totalDoDocumentistaPorStatus(usuarioId, StatusSolicitacaoDocumento.FINALIZADO_FORA_PRAZO));

        return new TotaisPorStatusDTO(aguardando, recebido, finalizado);
    }

    // =========================
    // HELPERS
    // =========================

    private void validarComentario(String comentario) {
        if (comentario == null || comentario.trim().length() < 3) {
            throw new RuntimeException("Comentário obrigatório (mínimo 3 caracteres).");
        }
    }

    private void validarAcao(AcaoSolicitacaoRequest request) {
        if (request == null) throw new RuntimeException("Payload inválido.");
        if (request.actorUsuarioId() == null || request.actorUsuarioId() <= 0) {
            throw new RuntimeException("actorUsuarioId é obrigatório.");
        }
        validarComentario(request.comentario());
    }

    private void validarFinalizacao(FinalizarSolicitacaoRequest request) {
        if (request == null) throw new RuntimeException("Payload inválido.");
        if (request.actorUsuarioId() == null || request.actorUsuarioId() <= 0) {
            throw new RuntimeException("actorUsuarioId é obrigatório.");
        }
        validarComentario(request.comentario());

        if (request.provaEnvio() == null || request.provaEnvio().trim().isEmpty()) {
            throw new RuntimeException("Prova de envio é obrigatória para finalizar.");
        }
    }

    private SolicitacaoDocumento buscarSolicitacaoOuFalhar(Long solicitacaoId) {
        return solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada."));
    }

    private Documento buscarDocumentoOuFalhar(Long documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado."));
    }

    private void registrarEvento(SolicitacaoDocumento solicitacao,
                                 TipoEventoSolicitacao tipo,
                                 StatusSolicitacaoDocumento anterior,
                                 StatusSolicitacaoDocumento novo,
                                 String comentario,
                                 Long actorUsuarioId) {

        SolicitacaoDocumentoEvento ev = new SolicitacaoDocumentoEvento();
        ev.setSolicitacaoId(solicitacao.getId());
        ev.setTipoEvento(tipo);
        ev.setStatusAnterior(anterior);
        ev.setStatusNovo(novo);
        ev.setComentario(comentario);
        ev.setActorUsuarioId(actorUsuarioId);

        eventoRepository.save(ev);
    }

    private BigDecimal buscarValorDoDocumentistaNoDocumento(SolicitacaoDocumento solicitacao, Long usuarioId) {
        if (solicitacao.getDocumento() == null || solicitacao.getDocumento().getPrecificacoes() == null) {
            return null;
        }

        return solicitacao.getDocumento().getPrecificacoes().stream()
                .filter(p -> Objects.equals(p.getUsuarioId(), usuarioId))
                .map(DocumentoPrecificacao::getValor)
                .findFirst()
                .orElse(null);
    }

}