package br.com.inproutservices.documentation_service.entities;

import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import br.com.inproutservices.documentation_service.enums.TipoEventoSolicitacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacao_documento_eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SolicitacaoDocumentoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="solicitacao_id", nullable = false)
    private Long solicitacaoId;

    @Enumerated(EnumType.STRING)
    @Column(name="tipo_evento", nullable = false, length = 40)
    private TipoEventoSolicitacao tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name="status_anterior", length = 40)
    private StatusSolicitacaoDocumento statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name="status_novo", length = 40)
    private StatusSolicitacaoDocumento statusNovo;

    @Column(nullable = false, length = 800)
    private String comentario;

    @Column(name="actor_usuario_id")
    private Long actorUsuarioId;

    @Column(name="criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }
}