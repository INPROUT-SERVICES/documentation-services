package br.com.inproutservices.documentation_service.services;

import br.com.inproutservices.documentation_service.client.MonolitoClient;
import br.com.inproutservices.documentation_service.dtos.*;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoEventoResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoListResponse;
import br.com.inproutservices.documentation_service.entities.Documento;
import br.com.inproutservices.documentation_service.entities.DocumentoPrecificacao;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumentoEvento;
import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import br.com.inproutservices.documentation_service.enums.TipoEventoSolicitacao;
import br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper;
import br.com.inproutservices.documentation_service.repositories.DocumentoRepository;
import br.com.inproutservices.documentation_service.repositories.SolicitacaoDocumentoEventoRepository;
import br.com.inproutservices.documentation_service.repositories.SolicitacaoDocumentoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UsuarioFacade usuarioFacade;
    private final DescontoService descontoService;

    private static final java.util.Set<String> DOCUMENTOS_PDI = java.util.Set.of(
            "PDI + CAD (PPI FORNECIDO)",
            "PDI COM PPI (SEM LAYOUT)",
            "PDI DIRETO SEM PPI (SEM LAYOUT)",
            "PDI SEM PPI + CAD"
    );

    // =========================
    // ENRIQUECIMENTO (FRONTEND)
    // =========================
    public SolicitacaoListResponse mapearParaResponse(SolicitacaoDocumento s) {
        String docNome = "Sem Responsável";
        String solNome = s.getSolicitanteNome() != null ? s.getSolicitanteNome() : "Sistema";

        if (s.getDocumentistaId() != null) {
            try {
                UsuarioDTO doc = usuarioFacade.buscarUsuario(s.getDocumentistaId());
                if (doc != null) docNome = doc.nome();
            } catch (Exception ignored) {}
        }
        return SolicitacaoMapper.toList(s, solNome, docNome);
    }

    // =========================
    // SOLICITAÇÃO
    // =========================

    @Transactional
    public SolicitacaoDocumento criarSolicitacao(Long osId,
                                                 Long documentoId,
                                                 Long documentistaId,
                                                 Long actorUsuarioId,
                                                 String comentario,
                                                 Set<Long> lancamentoIds,
                                                 String osNome,
                                                 String segmentoNome,
                                                 String solicitanteNome,
                                                 String site,
                                                 Boolean jaRecebido) {

        validarComentario(comentario);

        if (osId == null || osId <= 0) throw new RuntimeException("osId é obrigatório.");
        if (documentoId == null || documentoId <= 0) throw new RuntimeException("documentoId é obrigatório.");
        if (documentistaId == null || documentistaId <= 0) throw new RuntimeException("documentistaId é obrigatório.");
        if (actorUsuarioId == null || actorUsuarioId <= 0) throw new RuntimeException("actorUsuarioId é obrigatório.");

        // Normaliza site: null/blank → string vazia (evita problemas de NULL em UNIQUE constraint do PostgreSQL)
        String siteNormalizado = (site != null && !site.isBlank()) ? site.trim() : "";

        // Unicidade por OS + Site + Documento + Documentista
        if (solicitacaoRepository.existsByOsIdAndSiteAndDocumento_IdAndDocumentistaId(osId, siteNormalizado, documentoId, documentistaId)) {
            String msgSite = siteNormalizado.isEmpty() ? "" : " no site '" + siteNormalizado + "'";
            throw new RuntimeException("Já existe solicitação deste documento para o documentista" + msgSite + " nesta OS.");
        }

        Documento doc = buscarDocumentoOuFalhar(documentoId);

        if (!doc.isAtivo()) {
            throw new RuntimeException("Documento desativado não pode ser solicitado.");
        }

        if (doc.getDocumentistasIds() == null || !doc.getDocumentistasIds().contains(documentistaId)) {
            throw new RuntimeException("Documentista selecionado não está vinculado a este documento.");
        }

        String osCodigo;
        String projetoNome;
        String segmentoNomeReal;

        try {
            var osInfo = monolitoClient.buscarInfoOs(osId);

            if (osInfo == null) {
                throw new RuntimeException("A resposta do monolito retornou vazia.");
            }

            osCodigo = osInfo.os();
            projetoNome = osInfo.projeto();
            // Segmento vem do monolito (fonte confiável), ignora o que o frontend mandou
            segmentoNomeReal = osInfo.segmentoNome();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Não foi possível criar a solicitação pois houve uma falha de comunicação com o sistema principal (Monolito) " +
                            "ao buscar as informações da OS (ID: " + osId + "). " +
                            "Verifique se a OS existe e se o serviço está operante. Detalhe técnico: " + e.getMessage(), e
            );
        }

        SolicitacaoDocumento solicitacao = new SolicitacaoDocumento();
        solicitacao.setOsId(osId);
        solicitacao.setSite(siteNormalizado);
        solicitacao.setOs(osCodigo);
        solicitacao.setProjeto(projetoNome);
        solicitacao.setDocumento(doc);
        solicitacao.setDocumentistaId(documentistaId);
        solicitacao.setSolicitanteId(actorUsuarioId);
        solicitacao.setOsNome(osNome);
        solicitacao.setSegmentoNome(segmentoNomeReal);
        solicitacao.setSolicitanteNome(solicitanteNome);
        solicitacao.setAtivo(true);
        solicitacao.setStatus(StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        solicitacao.setProvaEnvio(null);
        solicitacao.setLancamentoIds(lancamentoIds);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(solicitacao);

        registrarEvento(salvo, TipoEventoSolicitacao.CRIADA, null, salvo.getStatus(), comentario, actorUsuarioId);

        // Se jaRecebido, muda direto para RECEBIDO (em análise)
        if (Boolean.TRUE.equals(jaRecebido)) {
            salvo.setStatus(StatusSolicitacaoDocumento.RECEBIDO);
            salvo.setRecebidoEm(LocalDateTime.now());
            salvo = solicitacaoRepository.save(salvo);

            registrarEvento(salvo, TipoEventoSolicitacao.MARCADO_RECEBIDO,
                    StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO,
                    StatusSolicitacaoDocumento.RECEBIDO,
                    "Marcado como recebido automaticamente na criação", actorUsuarioId);
        }

        monolitoClient.atualizarStatusLancamentos(new AtualizarLancamentosDocRequest(
                resolverLancamentoIdsParaUpdate(salvo),
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

        if (!Objects.equals(s.getDocumentistaId(), request.actorUsuarioId()) && !isUsuarioAdmin()) {
            throw new RuntimeException("Apenas o documentista atribuído ou um ADMIN pode finalizar esta solicitação.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();
        s.setProvaEnvio(request.provaEnvio().trim());
        s.setStatus(StatusSolicitacaoDocumento.FINALIZADO);
        s.setFinalizadoEm(LocalDateTime.now());

        // Calcular desconto se aplicável
        BigDecimal valorOriginal = br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper
                .valorDoDocumentistaNoDocumento(s, s.getDocumentistaId());
        descontoService.aplicarDesconto(s, valorOriginal);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.FINALIZADO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        if (salvo.getLancamentoIds() != null && !salvo.getLancamentoIds().isEmpty()) {
            monolitoClient.atualizarStatusLancamentos(new AtualizarLancamentosDocRequest(
                    resolverLancamentoIdsParaUpdate(salvo),
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

        if (!Objects.equals(s.getDocumentistaId(), request.actorUsuarioId()) && !isUsuarioAdmin()) {
            throw new RuntimeException("Apenas o documentista atribuído ou um ADMIN pode recusar esta solicitação.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();

        s.setStatus(StatusSolicitacaoDocumento.RECUSADO);
        s.setProvaEnvio(null);
        s.setFinalizadoEm(null);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.RECUSADO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        return salvo;
    }

    @Transactional
    public SolicitacaoDocumento resolicitar(Long solicitacaoId, AcaoSolicitacaoRequest request) {
        validarAcao(request);

        SolicitacaoDocumento s = buscarSolicitacao(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.RECUSADO) {
            throw new RuntimeException("Apenas solicitações RECUSADAS podem ser re-solicitadas.");
        }

        StatusSolicitacaoDocumento anterior = s.getStatus();

        s.setStatus(StatusSolicitacaoDocumento.RECEBIDO);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.RESOLICITADO, anterior, salvo.getStatus(),
                request.comentario(), request.actorUsuarioId());

        return salvo;
    }

    @Transactional
    public SolicitacaoDocumento editar(Long solicitacaoId, EditarSolicitacaoRequest request) {
        if (request == null) throw new RuntimeException("Payload inválido.");
        if (request.actorUsuarioId() == null || request.actorUsuarioId() <= 0) {
            throw new RuntimeException("actorUsuarioId é obrigatório.");
        }
        validarComentario(request.comentario());

        SolicitacaoDocumento s = buscarSolicitacao(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.RECUSADO) {
            throw new RuntimeException("Apenas solicitações RECUSADAS podem ser editadas.");
        }

        StringBuilder detalhes = new StringBuilder(request.comentario());
        boolean houveMudanca = false;

        // Guardar IDs antigos para liberar no monolito
        Set<Long> idsAntigos = s.getLancamentoIds() != null ? new java.util.HashSet<>(s.getLancamentoIds()) : Set.of();

        // Alterar documento
        if (request.documentoId() != null && !request.documentoId().equals(s.getDocumento().getId())) {
            Documento novoDoc = buscarDocumentoOuFalhar(request.documentoId());
            if (!novoDoc.isAtivo()) {
                throw new RuntimeException("Documento desativado não pode ser selecionado.");
            }
            String docAnterior = s.getDocumento().getNome();
            s.setDocumento(novoDoc);
            detalhes.append(" | Documento alterado de '").append(docAnterior).append("' para '").append(novoDoc.getNome()).append("'");
            houveMudanca = true;
        }

        // Alterar documentista
        if (request.documentistaId() != null && !request.documentistaId().equals(s.getDocumentistaId())) {
            Documento doc = s.getDocumento();
            if (doc.getDocumentistasIds() != null && !doc.getDocumentistasIds().contains(request.documentistaId())) {
                throw new RuntimeException("O documentista selecionado não está vinculado a este documento.");
            }

            String nomeAnterior = "ID:" + s.getDocumentistaId();
            try {
                UsuarioDTO anteriorUser = usuarioFacade.buscarUsuario(s.getDocumentistaId());
                if (anteriorUser != null) nomeAnterior = anteriorUser.nome();
            } catch (Exception ignored) {}

            String nomeNovo = "ID:" + request.documentistaId();
            try {
                UsuarioDTO novoUser = usuarioFacade.buscarUsuario(request.documentistaId());
                if (novoUser != null) nomeNovo = novoUser.nome();
            } catch (Exception ignored) {}

            s.setDocumentistaId(request.documentistaId());
            detalhes.append(" | Responsável alterado de '").append(nomeAnterior).append("' para '").append(nomeNovo).append("'");
            houveMudanca = true;
        }

        // Alterar itens (lancamentoIds)
        if (request.lancamentoIds() != null) {
            Set<Long> novosIds = request.lancamentoIds();
            if (!novosIds.equals(idsAntigos)) {
                int qtdAnterior = idsAntigos.size();
                int qtdNova = novosIds.size();
                s.setLancamentoIds(novosIds);
                detalhes.append(" | Itens alterados de ").append(qtdAnterior).append(" para ").append(qtdNova);
                houveMudanca = true;
            }
        }

        if (!houveMudanca) {
            throw new RuntimeException("Nenhuma alteração foi informada.");
        }

        // Volta para AGUARDANDO_RECEBIMENTO após edição
        StatusSolicitacaoDocumento anterior = s.getStatus();
        s.setStatus(StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        s.setProvaEnvio(null);
        s.setFinalizadoEm(null);

        SolicitacaoDocumento salvo = solicitacaoRepository.save(s);

        registrarEvento(salvo, TipoEventoSolicitacao.EDITADO, anterior, salvo.getStatus(),
                detalhes.toString(), request.actorUsuarioId());

        // Liberar itens removidos no monolito
        Set<Long> idsNovos = salvo.getLancamentoIds() != null ? salvo.getLancamentoIds() : Set.of();
        Set<Long> removidos = new java.util.HashSet<>(idsAntigos);
        removidos.removeAll(idsNovos);
        if (!removidos.isEmpty()) {
            try {
                monolitoClient.atualizarStatusLancamentos(new AtualizarLancamentosDocRequest(
                        removidos, null, null, null));
            } catch (Exception ignored) {}
        }

        // Marcar novos itens como NOK (com expansão PDI)
        Set<Long> idsParaUpdate = resolverLancamentoIdsParaUpdate(salvo);
        if (!idsParaUpdate.isEmpty()) {
            monolitoClient.atualizarStatusLancamentos(new AtualizarLancamentosDocRequest(
                    idsParaUpdate, "NOK", LocalDate.now().plusDays(2), "Aguardando documentação"));
        }

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

    // =========================
    // NOVO: HISTÓRICO COM INTEGRAÇÃO DE NOME
    // =========================
    public List<SolicitacaoEventoResponse> historicoEnriquecido(Long solicitacaoId) {
        if (solicitacaoId == null || solicitacaoId <= 0) throw new RuntimeException("solicitacaoId é obrigatório.");

        List<SolicitacaoDocumentoEvento> eventos = eventoRepository.findBySolicitacaoIdOrderByCriadoEmAsc(solicitacaoId);

        return eventos.stream().map(e -> {
            String actorNome = "Sistema";
            if (e.getActorUsuarioId() != null) {
                try {
                    UsuarioDTO user = usuarioFacade.buscarUsuario(e.getActorUsuarioId());
                    if (user != null && user.nome() != null) {
                        actorNome = user.nome();
                    }
                } catch (Exception ignored) {}
            }
            return SolicitacaoMapper.toEvento(e, actorNome);
        }).toList();
    }

    // =========================
    // LISTAGENS PAGINADAS
    // =========================

    public Page<SolicitacaoListResponse> pageTodas(List<String> segmentos, Pageable pageable) {
        if (segmentos != null && !segmentos.isEmpty()) return solicitacaoRepository.findBySegmentoNomeIn(segmentos, pageable).map(this::mapearParaResponse);
        return solicitacaoRepository.findAll(pageable).map(this::mapearParaResponse);
    }

    public Page<SolicitacaoListResponse> pagePorStatus(StatusSolicitacaoDocumento status, List<String> segmentos, Pageable pageable) {
        if (segmentos != null && !segmentos.isEmpty()) return solicitacaoRepository.findByStatusAndSegmentoNomeIn(status, segmentos, pageable).map(this::mapearParaResponse);
        return solicitacaoRepository.findByStatus(status, pageable).map(this::mapearParaResponse);
    }

    public Page<SolicitacaoListResponse> pagePorOs(Long osId, List<String> segmentos, Pageable pageable) {
        if (segmentos != null && !segmentos.isEmpty()) return solicitacaoRepository.findByOsIdAndSegmentoNomeIn(osId, segmentos, pageable).map(this::mapearParaResponse);
        return solicitacaoRepository.findByOsId(osId, pageable).map(this::mapearParaResponse);
    }

    public Page<SolicitacaoListResponse> pagePorOsEStatus(Long osId, StatusSolicitacaoDocumento status, List<String> segmentos, Pageable pageable) {
        if (segmentos != null && !segmentos.isEmpty()) return solicitacaoRepository.findByOsIdAndStatusAndSegmentoNomeIn(osId, status, segmentos, pageable).map(this::mapearParaResponse);
        return solicitacaoRepository.findByOsIdAndStatus(osId, status, pageable).map(this::mapearParaResponse);
    }

    public Page<SolicitacaoListResponse> pagePorDocumentista(Long documentistaId, List<String> segmentos, Pageable pageable) {
        if (segmentos != null && !segmentos.isEmpty()) return solicitacaoRepository.findByDocumentistaIdAndSegmentoNomeIn(documentistaId, segmentos, pageable).map(this::mapearParaResponse);
        return solicitacaoRepository.findByDocumentistaId(documentistaId, pageable).map(this::mapearParaResponse);
    }

    public Page<SolicitacaoListResponse> pagePorDocumentistaEStatus(Long documentistaId, StatusSolicitacaoDocumento status, List<String> segmentos, Pageable pageable) {
        if (segmentos != null && !segmentos.isEmpty()) return solicitacaoRepository.findByDocumentistaIdAndStatusAndSegmentoNomeIn(documentistaId, status, segmentos, pageable).map(this::mapearParaResponse);
        return solicitacaoRepository.findByDocumentistaIdAndStatus(documentistaId, status, pageable).map(this::mapearParaResponse);
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

    /**
     * Para documentos PDI, expande os lancamentoIds para TODOS os lançamentos aprovados da OS+Site.
     * Para outros documentos, retorna os lancamentoIds salvos na solicitação.
     */
    private Set<Long> resolverLancamentoIdsParaUpdate(SolicitacaoDocumento s) {
        if (s.getLancamentoIds() == null || s.getLancamentoIds().isEmpty()) {
            // Mesmo sem IDs selecionados, se for PDI, busca todos da OS+Site
            if (s.getDocumento() != null && DOCUMENTOS_PDI.contains(s.getDocumento().getNome())) {
                try {
                    return monolitoClient.buscarLancamentosPorOsSite(s.getOsId(), s.getSite() != null ? s.getSite() : "");
                } catch (Exception e) {
                    return Set.of();
                }
            }
            return Set.of();
        }

        if (s.getDocumento() != null && DOCUMENTOS_PDI.contains(s.getDocumento().getNome())) {
            try {
                Set<Long> todosDoSite = monolitoClient.buscarLancamentosPorOsSite(s.getOsId(), s.getSite() != null ? s.getSite() : "");
                return todosDoSite != null && !todosDoSite.isEmpty() ? todosDoSite : s.getLancamentoIds();
            } catch (Exception e) {
                return s.getLancamentoIds();
            }
        }

        return s.getLancamentoIds();
    }

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

    private boolean isUsuarioAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String authority = a.getAuthority();
                    return "ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority);
                });
    }

    @Transactional
    public void sincronizarOsEProjetoRetroativo() {
        List<SolicitacaoDocumento> solicitacoes = solicitacaoRepository.findAll();

        for (SolicitacaoDocumento s : solicitacoes) {
            boolean precisaSync = s.getOs() == null || s.getProjeto() == null
                    || s.getSegmentoNome() == null || s.getSegmentoNome().isBlank()
                    || "-".equals(s.getSegmentoNome())
                    || s.getSite() == null;  // Normaliza sites NULL para ""

            if (precisaSync) {
                try {
                    OsInfoDTO osInfo = monolitoClient.buscarInfoOs(s.getOsId());
                    if (osInfo != null) {
                        s.setOs(osInfo.os());
                        s.setProjeto(osInfo.projeto());
                        s.setSegmentoNome(osInfo.segmentoNome());
                    }
                    // Sempre normaliza site NULL → ""
                    if (s.getSite() == null) {
                        s.setSite("");
                    }
                    solicitacaoRepository.save(s);
                } catch (Exception e) {
                    System.err.println("Falha ao sincronizar OS ID " + s.getOsId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Retorna custos de documentação agrupados por osId.
     * "pago" = soma de valores de solicitações FINALIZADO ou FINALIZADO_FORA_PRAZO
     * "previsto" = soma de valores de TODAS as solicitações
     */
    public java.util.Map<Long, java.util.Map<String, BigDecimal>> custosPorOs(List<Long> osIds) {
        List<SolicitacaoDocumento> solicitacoes = solicitacaoRepository.findByOsIdIn(osIds);

        java.util.Map<Long, java.util.Map<String, BigDecimal>> resultado = new java.util.HashMap<>();

        for (SolicitacaoDocumento s : solicitacoes) {
            BigDecimal valor = SolicitacaoMapper.valorDoDocumentistaNoDocumento(s, s.getDocumentistaId());
            if (valor == null) valor = BigDecimal.ZERO;

            resultado.computeIfAbsent(s.getOsId(), k -> {
                java.util.Map<String, BigDecimal> m = new java.util.HashMap<>();
                m.put("pago", BigDecimal.ZERO);
                m.put("previsto", BigDecimal.ZERO);
                return m;
            });

            java.util.Map<String, BigDecimal> custos = resultado.get(s.getOsId());
            custos.put("previsto", custos.get("previsto").add(valor));

            if (s.getStatus() == StatusSolicitacaoDocumento.FINALIZADO
                    || s.getStatus() == StatusSolicitacaoDocumento.FINALIZADO_FORA_PRAZO) {
                custos.put("pago", custos.get("pago").add(valor));
            }
        }

        return resultado;
    }

    @Transactional
    public void renegociarDesconto(Long solicitacaoId, br.com.inproutservices.documentation_service.dtos.RenegociarDescontoRequest request) {
        SolicitacaoDocumento s = buscarSolicitacao(solicitacaoId);

        if (s.getStatus() != StatusSolicitacaoDocumento.FINALIZADO && s.getStatus() != StatusSolicitacaoDocumento.FINALIZADO_FORA_PRAZO) {
            throw new RuntimeException("Apenas solicitações finalizadas podem ter desconto renegociado.");
        }

        BigDecimal valorOriginal = br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper
                .valorDoDocumentistaNoDocumento(s, s.getDocumentistaId());
        if (valorOriginal == null) valorOriginal = BigDecimal.ZERO;

        BigDecimal novoPercentual = request.novoPercentualDesconto();
        if (novoPercentual == null || novoPercentual.compareTo(BigDecimal.ZERO) < 0) novoPercentual = BigDecimal.ZERO;
        if (novoPercentual.compareTo(new BigDecimal("0.50")) > 0) novoPercentual = new BigDecimal("0.50");

        BigDecimal desconto = valorOriginal.multiply(novoPercentual).setScale(2, java.math.RoundingMode.HALF_UP);
        s.setDescontoRenegociado(true);
        s.setPercentualDesconto(novoPercentual);
        s.setValorDesconto(desconto);
        s.setValorFinal(valorOriginal.subtract(desconto));

        solicitacaoRepository.save(s);

        registrarEvento(s, TipoEventoSolicitacao.DESCONTO_RENEGOCIADO, s.getStatus(), s.getStatus(),
                request.comentario(), request.actorUsuarioId());
    }
}