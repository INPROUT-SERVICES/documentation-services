package br.com.inproutservices.documentation_service.repositories;

import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumentoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoDocumentoEventoRepository extends JpaRepository<SolicitacaoDocumentoEvento, Long> {

    List<SolicitacaoDocumentoEvento> findBySolicitacaoIdOrderByCriadoEmAsc(Long solicitacaoId);

}
