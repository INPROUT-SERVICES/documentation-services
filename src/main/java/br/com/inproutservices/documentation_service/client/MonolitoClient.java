package br.com.inproutservices.documentation_service.client;

import br.com.inproutservices.documentation_service.dtos.AtualizarLancamentosDocRequest;
import br.com.inproutservices.documentation_service.dtos.OsInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inprout-monolito", url = "${monolito.url}")
public interface MonolitoClient {

    @PutMapping("/api/integracao/lancamentos/status-documentacao")
    void atualizarStatusLancamentos(@RequestBody AtualizarLancamentosDocRequest request);

    @GetMapping("/api/integracao/os/{osId}/info")
    OsInfoDTO buscarInfoOs(@PathVariable("osId") Long osId);
}