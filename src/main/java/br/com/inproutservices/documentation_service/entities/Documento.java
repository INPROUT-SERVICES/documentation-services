package br.com.inproutservices.documentation_service.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "documentacao")
@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(name = "valor_documentacao", precision = 10, scale = 2)
    private BigDecimal valorBase;

    @ElementCollection
    @CollectionTable(name = "documento_documentistas", joinColumns = @JoinColumn(name = "documento_id"))
    @Column(name = "usuario_id", nullable = false)
    private Set<Long> documentistasIds = new HashSet<>();

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<DocumentoPrecificacao> precificacoes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "data_criacao_doc", updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "data_atualizacao_doc")
    private LocalDateTime dataAtualizacao;

}
