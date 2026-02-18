package br.com.inproutservices.documentation_service.controllers;

import br.com.inproutservices.documentation_service.client.ClienteUsuario;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.toDetalhe;
import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.toEvento;
import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.toList;
import static br.com.inproutservices.documentation_service.mappers.SolicitacaoMapper.valorDoDocumentistaNoDocumento;

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoDocumentoController {

    private final SolicitacaoDocumentoService solicitacaoService;
    private final ClienteUsuario clienteUsuario;

    @PostMapping
    public ResponseEntity<SolicitacaoDetalheResponse> criar(@RequestBody CriarSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.criarSolicitacao(
                request.osId(),
                request.documentoId(),
                request.actorUsuarioId(),
                request.comentario()
        );

        SolicitacaoDetalheResponse resp = toDetalhe(s, Set.of(), null);

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/{id}/receber")
    public ResponseEntity<SolicitacaoDetalheResponse> marcarRecebido(@PathVariable Long id,
                                                                     @RequestBody AcaoSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.marcarRecebido(id, request);
        return ResponseEntity.ok(toDetalhe(s, Set.of(), null));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<SolicitacaoDetalheResponse> finalizar(@PathVariable Long id,
                                                                @RequestBody FinalizarSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.finalizar(id, request);
        return ResponseEntity.ok(toDetalhe(s, Set.of(), null));
    }

    @PostMapping("/{id}/recusar")
    public ResponseEntity<SolicitacaoDetalheResponse> recusar(@PathVariable Long id,
                                                              @RequestBody AcaoSolicitacaoRequest request) {
        SolicitacaoDocumento s = solicitacaoService.recusar(id, request);
        return ResponseEntity.ok(toDetalhe(s, Set.of(), null));
    }

    @PostMapping("/{id}/comentar")
    public ResponseEntity<Void> comentar(@PathVariable Long id,
                                         @RequestBody AcaoSolicitacaoRequest request) {
        solicitacaoService.comentar(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoListResponse>> listar(@RequestParam(name = "osId", required = false) Long osId,
                                                                @RequestParam(name = "status", required = false) StatusSolicitacaoDocumento status,
                                                                @RequestParam(name = "documentistaId", required = false) Long documentistaId) {

        List<SolicitacaoDocumento> solicitacoes;

        if (documentistaId != null && status != null) {
            solicitacoes = solicitacaoService.listarSolicitacoesDoDocumentistaPorStatus(documentistaId, status);
        } else if (documentistaId != null) {
            solicitacoes = solicitacaoService.listarSolicitacoesDoDocumentista(documentistaId);
        } else if (osId != null && status != null) {
            solicitacoes = solicitacaoService.listarPorOsEStatus(osId, status);
        } else if (osId != null) {
            solicitacoes = solicitacaoService.listarPorOs(osId);
        } else if (status != null) {
            solicitacoes = solicitacaoService.listarPorStatus(status);
        } else {
            solicitacoes = solicitacaoService.listarPorStatus(StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO);
        }

        List<SolicitacaoListResponse> resp = solicitacoes.stream().map(s -> toList(s)).toList();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoDetalheResponse> buscarDetalhe(@PathVariable Long id,
                                                                    @RequestParam(name = "includeDocumentistas", defaultValue = "false") boolean includeDocumentistas,
                                                                    @RequestParam(name = "usuarioId", required = false) Long usuarioId) {

        SolicitacaoDocumento s = solicitacaoService.buscarSolicitacao(id);

        Set<UsuarioDTO> documentistas = Set.of();

        if (includeDocumentistas && s.getDocumento() != null && s.getDocumento().getDocumentistasIds() != null) {
            documentistas = s.getDocumento().getDocumentistasIds().stream()
                    .map(clienteUsuario::buscarUsuario)
                    .collect(Collectors.toSet());
        }

        BigDecimal valor = null;
        if (usuarioId != null) {
            valor = valorDoDocumentistaNoDocumento(s, usuarioId);
        }

        return ResponseEntity.ok(toDetalhe(s, documentistas, valor));
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<SolicitacaoEventoResponse>> historico(@PathVariable Long id) {
        List<SolicitacaoDocumentoEvento> eventos = solicitacaoService.historico(id);
        List<SolicitacaoEventoResponse> resp = eventos.stream().map(e -> toEvento(e)).toList();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/documentistas/{usuarioId}/totais")
    public ResponseEntity<TotaisPorStatusDTO> totais(@PathVariable Long usuarioId) {
        TotaisPorStatusDTO totais = solicitacaoService.totaisDoDocumentistaPorStatus(usuarioId);
        return ResponseEntity.ok(totais);
    }
}