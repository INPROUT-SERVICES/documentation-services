package br.com.inproutservices.documentation_service.services;

import br.com.inproutservices.documentation_service.client.ClienteUsuario;
import br.com.inproutservices.documentation_service.dtos.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioFacade {

    private final ClienteUsuario clienteUsuario;

    @Cacheable(cacheNames = "usuarios", key = "#id")
    public UsuarioDTO buscarUsuario(Long id) {
        return clienteUsuario.buscarUsuario(id);
    }
}
