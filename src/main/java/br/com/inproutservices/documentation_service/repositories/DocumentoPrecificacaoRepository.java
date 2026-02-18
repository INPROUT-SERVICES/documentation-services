package br.com.inproutservices.documentation_service.repositories;

import br.com.inproutservices.documentation_service.entities.DocumentoPrecificacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoPrecificacaoRepository extends JpaRepository<DocumentoPrecificacao, Long> {
}
