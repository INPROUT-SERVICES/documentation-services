package br.com.inproutservices.documentation_service.dtos;

import java.util.Set;

public record UsuarioDTO(Long id, String nome, String email, Set<SegmentoDTO> segmentos){
}
