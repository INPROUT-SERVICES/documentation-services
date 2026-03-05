package br.com.inproutservices.documentation_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Value("${jwt.secret:bWV1cHJvamV0b2lucHJvdXRzZWd1cmFuY2EyMDI1Y29tY2hhdmVzdXBlcmZvcnRl}")
    private String secret;

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null && attributes.getRequest() != null) {
            String token = attributes.getRequest().getHeader("Authorization");
            if (token != null) {
                template.header("Authorization", token);
                return;
            }
        }

        String systemToken = gerarTokenDeSistema();
        template.header("Authorization", "Bearer " + systemToken);
    }

    private String gerarTokenDeSistema() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ROLE_ADMIN"); // Força o perfil admin para não ter bloqueio no Monolito

        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("sistema_docs_interno")
                .setIssuedAt(new Date(agora))
                .setExpiration(new Date(agora + 1000 * 60 * 5))
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }
}