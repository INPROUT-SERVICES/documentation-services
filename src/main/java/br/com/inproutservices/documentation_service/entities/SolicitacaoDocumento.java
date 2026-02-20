package br.com.inproutservices.documentation_service.entities;

import br.com.inproutservices.documentation_service.enums.StatusSolicitacaoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "solicitacoes_documento",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_os_documento", columnNames = {"os_id", "documento_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SolicitacaoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "documentista_id", nullable = false)
    private Long documentistaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusSolicitacaoDocumento status = StatusSolicitacaoDocumento.AGUARDANDO_RECEBIMENTO;

    @Column(name = "prova_envio", length = 500)
    private String provaEnvio;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "recebido_em")
    private LocalDateTime recebidoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @ElementCollection
    @CollectionTable(name = "solicitacao_documento_lancamentos", joinColumns = @JoinColumn(name = "solicitacao_id"))
    @Column(name = "lancamento_id")
    private Set<Long> lancamentoIds;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}