package br.com.inproutservices.documentation_service.services;

import br.com.inproutservices.documentation_service.entities.ConfiguracaoDocumentacao;
import br.com.inproutservices.documentation_service.entities.SolicitacaoDocumento;
import br.com.inproutservices.documentation_service.repositories.ConfiguracaoDocumentacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DescontoService {

    private static final String CHAVE_DESCONTO = "DESCONTO_ATIVO";
    private static final BigDecimal TAXA_POR_DIA = new BigDecimal("0.10"); // 10% por dia útil
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("0.50"); // máximo 50%

    private final ConfiguracaoDocumentacaoRepository configRepository;

    public boolean isDescontoAtivo() {
        return configRepository.findByChave(CHAVE_DESCONTO)
                .map(c -> "true".equalsIgnoreCase(c.getValor()))
                .orElse(false);
    }

    @Transactional
    public void toggleDesconto(boolean ativo) {
        ConfiguracaoDocumentacao config = configRepository.findByChave(CHAVE_DESCONTO)
                .orElseGet(() -> {
                    ConfiguracaoDocumentacao nova = new ConfiguracaoDocumentacao();
                    nova.setChave(CHAVE_DESCONTO);
                    return nova;
                });
        config.setValor(String.valueOf(ativo));
        config.setAtualizadoEm(LocalDateTime.now());
        configRepository.save(config);
    }

    /**
     * Calcula o desconto para uma solicitação finalizada.
     * Retorna o percentual aplicado (0.0 a 0.50).
     */
    public BigDecimal calcularPercentualDesconto(SolicitacaoDocumento s) {
        if (s.getPrazoEntrega() == null || s.getFinalizadoEm() == null) return BigDecimal.ZERO;
        if (!isDescontoAtivo()) return BigDecimal.ZERO;
        if (Boolean.TRUE.equals(s.getDescontoRenegociado())) return s.getPercentualDesconto() != null ? s.getPercentualDesconto() : BigDecimal.ZERO;

        if (!s.getFinalizadoEm().isAfter(s.getPrazoEntrega())) return BigDecimal.ZERO;

        long diasUteis = contarDiasUteis(s.getPrazoEntrega(), s.getFinalizadoEm());
        if (diasUteis <= 0) return BigDecimal.ZERO;

        BigDecimal percentual = TAXA_POR_DIA.multiply(BigDecimal.valueOf(diasUteis));
        return percentual.min(LIMITE_MAXIMO);
    }

    /**
     * Aplica o desconto na solicitação (seta valorDesconto, percentualDesconto, valorFinal).
     */
    public void aplicarDesconto(SolicitacaoDocumento s, BigDecimal valorOriginal) {
        if (valorOriginal == null || valorOriginal.compareTo(BigDecimal.ZERO) <= 0) {
            s.setValorDesconto(BigDecimal.ZERO);
            s.setPercentualDesconto(BigDecimal.ZERO);
            s.setValorFinal(valorOriginal);
            return;
        }

        BigDecimal percentual = calcularPercentualDesconto(s);
        BigDecimal desconto = valorOriginal.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorFinal = valorOriginal.subtract(desconto);

        s.setPercentualDesconto(percentual);
        s.setValorDesconto(desconto);
        s.setValorFinal(valorFinal);
    }

    /**
     * Conta dias úteis entre duas datas (exclui sábado e domingo).
     */
    public static long contarDiasUteis(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) return 0;

        long count = 0;
        LocalDateTime current = inicio.plusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime limitDate = fim.toLocalDate().atStartOfDay();

        while (!current.isAfter(limitDate)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}
