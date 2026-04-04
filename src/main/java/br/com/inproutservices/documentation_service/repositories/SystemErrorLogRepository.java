package br.com.inproutservices.documentation_service.repositories;

import br.com.inproutservices.documentation_service.entities.SystemErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemErrorLogRepository extends JpaRepository<SystemErrorLog, Long> {

    Page<SystemErrorLog> findAllByOrderByDataHoraDesc(Pageable pageable);

    @Query("SELECT s FROM SystemErrorLog s WHERE " +
           "(:statusCode IS NULL OR s.statusCode = :statusCode) AND " +
           "(:busca IS NULL OR :busca = '' OR " +
           "LOWER(s.uri) LIKE LOWER(CONCAT('%', :busca, '%')) OR " +
           "LOWER(s.usuarioEmail) LIKE LOWER(CONCAT('%', :busca, '%')) OR " +
           "LOWER(s.mensagem) LIKE LOWER(CONCAT('%', :busca, '%'))) " +
           "ORDER BY s.dataHora DESC")
    Page<SystemErrorLog> buscarComFiltros(@Param("statusCode") Integer statusCode,
                                          @Param("busca") String busca,
                                          Pageable pageable);
}
