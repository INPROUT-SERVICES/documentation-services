package br.com.inproutservices.documentation_service.controllers;

import br.com.inproutservices.documentation_service.dtos.AtualizarDocumentoRequest;
import br.com.inproutservices.documentation_service.dtos.CriarDocumentoRequest;
import br.com.inproutservices.documentation_service.dtos.PrecificarDocumentoRequest;
import br.com.inproutservices.documentation_service.dtos.responses.DocumentoDetalheResponse;
import br.com.inproutservices.documentation_service.dtos.responses.DocumentoListResponse;
import br.com.inproutservices.documentation_service.entities.Documento;
import br.com.inproutservices.documentation_service.services.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static br.com.inproutservices.documentation_service.mappers.DocumentoMapper.toDetalhe;
import static br.com.inproutservices.documentation_service.mappers.DocumentoMapper.toList;

@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @PostMapping
    public ResponseEntity<DocumentoDetalheResponse> criar(@RequestBody CriarDocumentoRequest request) {
        Documento doc = documentoService.criarDocumento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetalhe(doc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoDetalheResponse> alterar(@PathVariable Long id,
                                                            @RequestBody AtualizarDocumentoRequest request) {
        Documento doc = documentoService.alterarDocumento(id, request);
        return ResponseEntity.ok(toDetalhe(doc));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        documentoService.desativarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        documentoService.ativarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/precificar")
    public ResponseEntity<DocumentoDetalheResponse> precificar(@PathVariable Long id,
                                                               @RequestBody PrecificarDocumentoRequest request) {
        Documento doc = documentoService.precificarDocumento(id, request);
        return ResponseEntity.ok(toDetalhe(doc));
    }

    @GetMapping
    public ResponseEntity<List<DocumentoListResponse>> listar(@RequestParam(name = "somenteAtivos", defaultValue = "false") boolean somenteAtivos) {
        List<Documento> docs = somenteAtivos
                ? documentoService.listarDocumentosAtivos()
                : documentoService.listarTodosDocumentos();

        List<DocumentoListResponse> resp = docs.stream().map(d -> toList(d)).toList();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDetalheResponse> buscarDetalhe(@PathVariable Long id) {
        Documento doc = documentoService.buscarDocumento(id);
        return ResponseEntity.ok(toDetalhe(doc));
    }
}