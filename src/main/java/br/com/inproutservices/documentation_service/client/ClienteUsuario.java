package br.com.inproutservices.documentation_service.client;

import br.com.inproutservices.documentation_service.dtos.UsuarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "usuario-service", url = "${usuario.service.url}")
public interface ClienteUsuario {

    @GetMapping("/usuarios/{id}")
    UsuarioDTO buscarUsuario(@PathVariable Long id);
}