package br.com.inproutservices.documentation_service.controllers;

import br.com.inproutservices.documentation_service.repositories.SystemErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class SystemErrorLogController {

    private final SystemErrorLogRepository systemErrorLogRepo;

    @GetMapping("/erros")
    public ResponseEntity<?> auditErros(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String busca) {

        var pageable = PageRequest.of(page, size);
        boolean temFiltro = (statusCode != null) || (busca != null && !busca.isBlank());

        if (temFiltro) {
            return ResponseEntity.ok(systemErrorLogRepo.buscarComFiltros(
                    statusCode,
                    busca != null && !busca.isBlank() ? busca : null,
                    pageable));
        }
        return ResponseEntity.ok(systemErrorLogRepo.findAllByOrderByDataHoraDesc(pageable));
    }
}
