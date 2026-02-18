package br.com.inproutservices.documentation_service.repositories;

import br.com.inproutservices.documentation_service.entities.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
}
