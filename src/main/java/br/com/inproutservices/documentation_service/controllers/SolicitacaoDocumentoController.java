package br.com.inproutservices.documentation_service.controllers;

import br.com.inproutservices.documentation_service.dtos.*;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoDetalheResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoEventoResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoListResponse;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import br.com.inproutservices.documentation_service.services.SolicitacaoDocumentoService;
import br.com.inproutservices.documentation_service.services.UsuarioFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.toDetalhe;
import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.valorDoDocumentistaNoDocumento;

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoDocumentoController {

    private final SolicitacaoDocumentoService solicitacaoService;
    private final UsuarioFacade usuarioFacade;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<SolicitacaoDetalheResponse> criar(@RequestBody CriarSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.criarSolicitacao(
                request.osId(),
                request.documentoId(),
                request.documentistaId(),
                request.actorUsuarioId(),
                request.comentario(),
                request.lancamentoIds(),
                request.osNome(),
                request.segmentoNome(),
                request.solicitanteNome(),
                request.site(),
                request.jaRecebido()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetalhe(s, null, null));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping("/{id}/receber")
    public ResponseEntity<SolicitacaoDetalheResponse> marcarRecebido(@PathVariable Long id,
                                                                     @RequestBody AcaoSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.marcarRecebido(id, request);
        return ResponseEntity.ok(toDetalhe(s, null, null));
    }

    @PreAuthorize("hasAnyRole('DOCUMENTIST','ADMIN')")
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<SolicitacaoDetalheResponse> finalizar(@PathVariable Long id,
                                                                @RequestBody FinalizarSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.finalizar(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetalhe(s, null, null));
    }

    @PreAuthorize("hasAnyRole('DOCUMENTIST','ADMIN')")
    @PostMapping("/{id}/recusar")
    public ResponseEntity<SolicitacaoDetalheResponse> recusar(@PathVariable Long id,
                                                              @RequestBody AcaoSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.recusar(id, request);
        return ResponseEntity.ok(toDetalhe(s, null, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR','MANAGER')")
    @PostMapping("/{id}/resolicitar")
    public ResponseEntity<SolicitacaoDetalheResponse> resolicitar(@PathVariable Long id,
                                                                   @RequestBody AcaoSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.resolicitar(id, request);
        return ResponseEntity.ok(toDetalhe(s, null, null));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/{id}/editar")
    public ResponseEntity<SolicitacaoDetalheResponse> editar(@PathVariable Long id,
                                                              @RequestBody EditarSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.editar(id, request);
        return ResponseEntity.ok(toDetalhe(s, null, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER', 'ASSISTANT', 'VISUALIZADOR')")
    @PostMapping("/{id}/comentar")
    public ResponseEntity<Void> comentar(@PathVariable Long id,
                                         @RequestBody AcaoSolicitacaoRequest request) {
        solicitacaoService.comentar(id, request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER')")
    @PatchMapping("/{id}/renegociar-desconto")
    public ResponseEntity<Void> renegociarDesconto(@PathVariable Long id,
                                                   @RequestBody br.com.inproutservices.documentation_service.dtos.RenegociarDescontoRequest request) {
        solicitacaoService.renegociarDesconto(id, request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER', 'ASSISTANT', 'VISUALIZADOR')")
    @GetMapping
    public ResponseEntity<Page<SolicitacaoListResponse>> listar(@RequestParam(name = "osId", required = false) Long osId,
                                                                @RequestParam(name = "status", required = false) StatusSolicitacaoDocumento status,
                                                                @RequestParam(name = "documentistaId", required = false) Long documentistaId,
                                                                @RequestParam(name = "usuarioId", required = false) Long usuarioId,
                                                                Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> segmentosFiltro = null;

        // Se o usuário for MANAGER ou COORDINATOR, ativamos a busca dos seus segmentos
        // Aceita com ou sem prefixo ROLE_ (depende da config do Spring Security)
        boolean filtraPorSegmento = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String authority = a.getAuthority();
                    return "MANAGER".equals(authority) || "ROLE_MANAGER".equals(authority)
                            || "COORDINATOR".equals(authority) || "ROLE_COORDINATOR".equals(authority);
                });

        if (filtraPorSegmento && usuarioId != null) {
            UsuarioDTO user = usuarioFacade.buscarUsuario(usuarioId);
            if (user != null && user.segmentos() != null) {
                segmentosFiltro = user.segmentos().stream().map(SegmentoDTO::nome).toList();
            }
        }

        Page<SolicitacaoListResponse> page;

        if (documentistaId != null && status != null) {
            page = solicitacaoService.pagePorDocumentistaEStatus(documentistaId, status, segmentosFiltro, pageable);
        } else if (documentistaId != null) {
            page = solicitacaoService.pagePorDocumentista(documentistaId, segmentosFiltro, pageable);
        } else if (osId != null && status != null) {
            page = solicitacaoService.pagePorOsEStatus(osId, status, segmentosFiltro, pageable);
        } else if (osId != null) {
            page = solicitacaoService.pagePorOs(osId, segmentosFiltro, pageable);
        } else if (status != null) {
            page = solicitacaoService.pagePorStatus(status, segmentosFiltro, pageable);
        } else {
            page = solicitacaoService.pageTodas(segmentosFiltro, pageable);
        }

        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER', 'ASSISTANT', 'VISUALIZADOR')")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoDetalheResponse> buscarDetalhe(@PathVariable Long id,
                                                                    @RequestParam(name = "includeDocumentista", defaultValue = "false") boolean includeDocumentista,
                                                                    @RequestParam(name = "usuarioId", required = false) Long usuarioId) {

        SolicitacaoDocumento s = solicitacaoService.buscarSolicitacao(id);

        UsuarioDTO documentista = null;

        if (includeDocumentista) {
            documentista = usuarioFacade.buscarUsuario(s.getDocumentistaId());
        }

        BigDecimal valor = null;
        if (usuarioId != null) {
            valor = valorDoDocumentistaNoDocumento(s, usuarioId);
        }

        return ResponseEntity.ok(toDetalhe(s, documentista, valor));
    }

    // =====================================
    // ATUALIZADO: Chama o metodo Enriquecido
    // =====================================
    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER', 'ASSISTANT', 'VISUALIZADOR')")
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<SolicitacaoEventoResponse>> historico(@PathVariable Long id) {
        List<SolicitacaoEventoResponse> resp = solicitacaoService.historicoEnriquecido(id);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER', 'VISUALIZADOR')")
    @GetMapping("/documentistas/{documentistaId}/totais")
    public ResponseEntity<TotaisPorStatusDTO> totais(@PathVariable Long documentistaId) {
        TotaisPorStatusDTO totais = solicitacaoService.totaisDoDocumentistaPorStatus(documentistaId);
        return ResponseEntity.ok(totais);
    }

    @PostMapping("/sync-legado-os-projeto")
    public ResponseEntity<String> syncRetroativo() {
        solicitacaoService.sincronizarOsEProjetoRetroativo();
        return ResponseEntity.ok("Sincronização iniciada/concluída com sucesso.");
    }

    @GetMapping("/custos-por-os")
    public ResponseEntity<java.util.Map<Long, java.util.Map<String, java.math.BigDecimal>>> custosPorOs(
            @RequestParam("osIds") java.util.List<Long> osIds) {
        return ResponseEntity.ok(solicitacaoService.custosPorOs(osIds));
    }
}