package br.com.inproutservices.documentation_service.controllers;

import br.com.inproutservices.documentation_service.dtos.AcaoSolicitacaoRequest;
import br.com.inproutservices.documentation_service.dtos.CriarSolicitacaoRequest;
import br.com.inproutservices.documentation_service.dtos.FinalizarSolicitacaoRequest;
import br.com.inproutservices.documentation_service.dtos.TotaisPorStatusDTO;
import br.com.inproutservices.documentation_service.dtos.UsuarioDTO;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoDetalheResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoEventoResponse;
import br.com.inproutservices.documentation_service.dtos.responses.SolicitacaoListResponse;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumentoEvento;
import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import br.com.inproutservices.documentation_service.services.SolicitacaoDocumentoService;
import br.com.inproutservices.documentation_service.services.UsuarioFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.toDetalhe;
import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.toEvento;
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
                request.lancamentoIds()
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

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER')")
    @PostMapping("/{id}/comentar")
    public ResponseEntity<Void> comentar(@PathVariable Long id,
                                         @RequestBody AcaoSolicitacaoRequest request) {
        solicitacaoService.comentar(id, request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER')")
    @GetMapping
    public ResponseEntity<Page<SolicitacaoListResponse>> listar(@RequestParam(name = "osId", required = false) Long osId,
                                                                @RequestParam(name = "status", required = false) StatusSolicitacaoDocumento status,
                                                                @RequestParam(name = "documentistaId", required = false) Long documentistaId,
                                                                Pageable pageable) {

        Page<SolicitacaoListResponse> page;

        if (documentistaId != null && status != null) {
            page = solicitacaoService.pagePorDocumentistaEStatus(documentistaId, status, pageable);
        } else if (documentistaId != null) {
            page = solicitacaoService.pagePorDocumentista(documentistaId, pageable);
        } else if (osId != null && status != null) {
            page = solicitacaoService.pagePorOsEStatus(osId, status, pageable);
        } else if (osId != null) {
            page = solicitacaoService.pagePorOs(osId, pageable);
        } else if (status != null) {
            page = solicitacaoService.pagePorStatus(status, pageable);
        } else {
            page = solicitacaoService.pageTodas(pageable);
        }

        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER')")
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

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER')")
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<SolicitacaoEventoResponse>> historico(@PathVariable Long id) {
        List<SolicitacaoDocumentoEvento> eventos = solicitacaoService.historico(id);
        List<SolicitacaoEventoResponse> resp = eventos.stream().map(e -> toEvento(e)).toList();
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER','COORDINATOR','DOCUMENTIST','MANAGER')")
    @GetMapping("/documentistas/{documentistaId}/totais")
    public ResponseEntity<TotaisPorStatusDTO> totais(@PathVariable Long documentistaId) {
        TotaisPorStatusDTO totais = solicitacaoService.totaisDoDocumentistaPorStatus(documentistaId);
        return ResponseEntity.ok(totais);
    }
}