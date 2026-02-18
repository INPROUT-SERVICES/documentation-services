package br.com.inproutservices.documentation_service.repositories;

import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitacaoDocumentoRepository extends JpaRepository<SolicitacaoDocumento, Long> {

    List<SolicitacaoDocumento> findByStatus(StatusSolicitacaoDocumento status);

    List<SolicitacaoDocumento> findByOsId(Long osId);

    List<SolicitacaoDocumento> findByOsIdAndStatus(Long osId, StatusSolicitacaoDocumento status);

    boolean existsByOsIdAndDocumento_Id(Long osId, Long documentoId);

    @Query("""
       SELECT s
       FROM SolicitacaoDocumento s
       JOIN s.documento d
       WHERE :usuarioId MEMBER OF d.documentistasIds
       """)
    List<SolicitacaoDocumento> listarPorDocumentista(@Param("usuarioId") Long usuarioId);

    @Query("""
       SELECT s
       FROM SolicitacaoDocumento s
       JOIN s.documento d
       WHERE s.status = :status
         AND :usuarioId MEMBER OF d.documentistasIds
       """)
    List<SolicitacaoDocumento> listarPorDocumentistaEStatus(@Param("usuarioId") Long usuarioId,
                                                            @Param("status") StatusSolicitacaoDocumento status);
}
