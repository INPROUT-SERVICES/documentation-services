package br.com.inproutservices.documentation_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_error_log")
@Getter
@Setter
@NoArgsConstructor
public class SystemErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "metodo_http")
    private String metodoHttp;

    private String uri;

    @Column(name = "usuario_email")
    private String usuarioEmail;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "erro_tipo")
    private String erroTipo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @PrePersist
    protected void onCreate() {
        this.dataHora = LocalDateTime.now();
    }
}
