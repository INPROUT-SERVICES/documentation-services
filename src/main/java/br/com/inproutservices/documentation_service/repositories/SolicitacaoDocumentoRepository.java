package br.com.inproutservices.documentation_service.repositories;

import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SolicitacaoDocumentoRepository extends JpaRepository<SolicitacaoDocumento, Long> {

    List<SolicitacaoDocumento> findByStatus(StatusSolicitacaoDocumento status);

    List<SolicitacaoDocumento> findByOsId(Long osId);

    List<SolicitacaoDocumento> findByOsIdAndStatus(Long osId, StatusSolicitacaoDocumento status);

    List<SolicitacaoDocumento> findByDocumentistaId(Long documentistaId);

    List<SolicitacaoDocumento> findByDocumentistaIdAndStatus(Long documentistaId, StatusSolicitacaoDocumento status);

    Page<SolicitacaoDocumento> findAll(Pageable pageable);
    Page<SolicitacaoDocumento> findByStatus(StatusSolicitacaoDocumento status, Pageable pageable);
    Page<SolicitacaoDocumento> findByOsId(Long osId, Pageable pageable);
    Page<SolicitacaoDocumento> findByOsIdAndStatus(Long osId, StatusSolicitacaoDocumento status, Pageable pageable);
    Page<SolicitacaoDocumento> findByDocumentistaId(Long documentistaId, Pageable pageable);
    Page<SolicitacaoDocumento> findByDocumentistaIdAndStatus(Long documentistaId, StatusSolicitacaoDocumento status, Pageable pageable);

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

    Page<SolicitacaoDocumento> findBySegmentoNomeIn(List<String> segmentoNome, Pageable pageable);
    Page<SolicitacaoDocumento> findByStatusAndSegmentoNomeIn(StatusSolicitacaoDocumento status, List<String> segmentoNome, Pageable pageable);
    Page<SolicitacaoDocumento> findByOsIdAndSegmentoNomeIn(Long osId, List<String> segmentoNome, Pageable pageable);
    Page<SolicitacaoDocumento> findByOsIdAndStatusAndSegmentoNomeIn(Long osId, StatusSolicitacaoDocumento status, List<String> segmentoNome, Pageable pageable);
    Page<SolicitacaoDocumento> findByDocumentistaIdAndSegmentoNomeIn(Long documentistaId, List<String> segmentoNome, Pageable pageable);
    Page<SolicitacaoDocumento> findByDocumentistaIdAndStatusAndSegmentoNomeIn(Long documentistaId, StatusSolicitacaoDocumento status, List<String> segmentoNome, Pageable pageable);
}
