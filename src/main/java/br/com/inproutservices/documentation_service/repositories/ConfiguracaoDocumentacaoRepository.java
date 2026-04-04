package br.com.inproutservices.documentation_service.repositories;

import br.com.inproutservices.documentation_service.entities.ConfiguracaoDocumentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracaoDocumentacaoRepository extends JpaRepository<ConfiguracaoDocumentacao, Long> {
    Optional<ConfiguracaoDocumentacao> findByChave(String chave);
}
