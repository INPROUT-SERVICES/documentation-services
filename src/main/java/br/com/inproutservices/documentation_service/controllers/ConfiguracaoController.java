package br.com.inproutservices.documentation_service.controllers;

import br.com.inproutservices.documentation_service.services.DescontoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final DescontoService descontoService;

    @GetMapping("/desconto-toggle")
    public ResponseEntity<Map<String, Object>> getDescontoToggle() {
        return ResponseEntity.ok(Map.of("ativo", descontoService.isDescontoAtivo()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','CONTROLLER')")
    @PostMapping("/desconto-toggle")
    public ResponseEntity<Map<String, Object>> toggleDesconto(@RequestBody Map<String, Boolean> payload) {
        boolean ativo = payload.getOrDefault("ativo", false);
        descontoService.toggleDesconto(ativo);
        return ResponseEntity.ok(Map.of("ativo", ativo, "mensagem", "Desconto " + (ativo ? "ativado" : "desativado") + " com sucesso."));
    }
}
