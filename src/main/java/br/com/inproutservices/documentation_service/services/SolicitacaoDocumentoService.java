package br.com.inproutservices.documentation_service.services;

import br.com.inproutservices.documentation_service.client.MonolitoClient;
import br.com.inproutservices.documentation_service.dtos.AcaoSolicitacaoRequest;
import br.com.inproutservices.documentation_service.dtos.AtualizarLancamentosDocRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SolicitacaoDocumentoService {

    private final SolicitacaoDocumentoRepository solicitacaoRepository;
    private final SolicitacaoDocumentoEventoRepository eventoRepository;
    private final DocumentoRepository documentoRepository;
    private final MonolitoClient monolitoClient;

    // =========================
    // SOLICITAÇÃO
    // =========================

    @Transactional
    public SolicitacaoDocumento criarSolicitacao(Long osId,
                                                 Long documentoId,
                                                 Long documentistaId,
                                                 Long actorUsuarioId,
                                                 String comentario,
                                                 Set<Long> lancamentoIds) {

        validarComentario(comentario);

        if (osId == null || osId <= 0) throw new RuntimeException("osId é obrigatório.");
        if (documentoId == null || documentoId <= 0) throw new RuntimeException("documentoId é obrigatório.");
        if (documentistaId == null || documentistaId <= 0) throw new RuntimeException("documentistaId é obrigatório.");
        if (actorUsuarioId == null || actorUsuarioId <= 0) throw new RuntimeException("actorUsuarioId é obrigatório.");

        if (solicitacaoRepository.existsByOsIdAndDocumento_Id(osId, documentoId)) {
            throw new RuntimeException("Já existe solicitação deste documento para esta OS.");
        }

        Documento doc = buscarDocumentoOuFalhar(documentoId);

        if (!doc.isAtivo()) {
            throw new RuntimeException("Documento desativado não pode ser solicitado.");
        }

        if (doc.getDocumentistasIds() == null || !doc.getDocumentistasIds().contains(documentistaId)) {
            throw new RuntimeException("Documentista selecionado não está vinculado a este documento.");
        }

        SolicitacaoDocumento solicitacao = new SolicitacaoDocumento();
        solicitacao.setOsId(osId);
        solicitacao.setDocumento(doc);
        solicitacao.setDocumentistaId(documentistaId);
        solicitacao.setAtivo(true);
        solicitacao.setStatus(StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        solicitacao.setProvaEnvio(null);
        solicitacao.setLancamentoIds(lancamentoIds);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(solicitacao);

        registrarEvento(salvo, TipoEventoSolicitacao.CRIADA, null, salvo.getStatus(), comentario, actorUsuarioId);

        monolitoClient.atualizarStatusLancamentos(new AtualizarLancamentosDocRequest(
                salvo.getLancamentoIds(),
                "NOK",
                LocalDate.now().plusDays(2),
                "Aguardando documentação"
        ));

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

        SolicitacaoDocumento s = buscarSolicitacao(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO) {
            throw new RuntimeException("Solicitação não está aguardando recebimento.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();
        s.setStatus(StatusSolicitacaoDocumento.RECEBIDO);
        s.setRecebidoEm(LocalDateTime.now());

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.MARCADO_RECEBIDO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        return salvo;
    }

    @Transactional
    public SolicitacaoDocumento finalizar(Long solicitacaoId, FinalizarSolicitacaoRequest request) {
        validarFinalizacao(request);

        SolicitacaoDocumento s = buscarSolicitacao(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.RECEBIDO) {
            throw new RuntimeException("Solicitação precisa estar RECEBIDO para finalizar.");
        }

        if (!Objects.equals(s.getDocumentistaId(), request.actorUsuarioId())) {
            throw new RuntimeException("Apenas o documentista atribuído pode finalizar esta solicitação.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();
        s.setProvaEnvio(request.provaEnvio().trim());
        s.setStatus(StatusSolicitacaoDocumento.FINALIZADO);
        s.setFinalizadoEm(LocalDateTime.now());

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.FINALIZADO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        if (salvo.getLancamentoIds() != null && !salvo.getLancamentoIds().isEmpty()) {
            monolitoClient.atualizarStatusLancamentos(new AtualizarLancamentosDocRequest(
                    salvo.getLancamentoIds(),
                    "OK",
                    LocalDate.now(),
                    "Finalizado"
            ));
        }

        return salvo;
    }

    @Transactional
    public SolicitacaoDocumento recusar(Long solicitacaoId, AcaoSolicitacaoRequest request) {
        validarAcao(request);

        SolicitacaoDocumento s = buscarSolicitacao(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.RECEBIDO) {
            throw new RuntimeException("Solicitação precisa estar RECEBIDO para recusar.");
        }

        if (!Objects.equals(s.getDocumentistaId(), request.actorUsuarioId())) {
            throw new RuntimeException("Apenas o documentista atribuído pode recusar esta solicitação.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();

        s.setStatus(StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        s.setProvaEnvio(null);
        s.setRecebidoEm(null);
        s.setFinalizadoEm(null);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.RECUSADO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        return salvo;
    }

    @Transactional
    public void comentar(Long solicitacaoId, AcaoSolicitacaoRequest request) {
        validarAcao(request);

        SolicitacaoDocumento s = buscarSolicitacao(solicitacaoId);

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

    public Page<SolicitacaoDocumento> pageTodas(Pageable pageable) {
        return solicitacaoRepository.findAll(pageable);
    }

    public Page<SolicitacaoDocumento> pagePorStatus(StatusSolicitacaoDocumento status, Pageable pageable) {
        return solicitacaoRepository.findByStatus(status, pageable);
    }

    public Page<SolicitacaoDocumento> pagePorOs(Long osId, Pageable pageable) {
        return solicitacaoRepository.findByOsId(osId, pageable);
    }

    public Page<SolicitacaoDocumento> pagePorOsEStatus(Long osId, StatusSolicitacaoDocumento status, Pageable pageable) {
        return solicitacaoRepository.findByOsIdAndStatus(osId, status, pageable);
    }

    public Page<SolicitacaoDocumento> pagePorDocumentista(Long documentistaId, Pageable pageable) {
        return solicitacaoRepository.findByDocumentistaId(documentistaId, pageable);
    }

    public Page<SolicitacaoDocumento> pagePorDocumentistaEStatus(Long documentistaId, StatusSolicitacaoDocumento status, Pageable pageable) {
        return solicitacaoRepository.findByDocumentistaIdAndStatus(documentistaId, status, pageable);
    }

}