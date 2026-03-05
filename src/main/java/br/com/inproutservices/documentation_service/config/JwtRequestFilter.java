package br.com.inproutservices.documentation_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:bWV1cHJvamV0b2lucHJvdXRzZWd1cmFuY2EyMDI1Y29tY2hhdmVzdXBlcmZvcnRl}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.replace("Bearer ", "").trim();

        try {
            // Decodifica a chave e prepara para a validação do JJWT
            byte[] secretBytes = Base64.getDecoder().decode(secret);
            SecretKey key = Keys.hmacShaKeyFor(secretBytes);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String subject = claims.getSubject();

            if (subject != null && !subject.trim().isEmpty()) {

                Collection<? extends GrantedAuthority> authorities = extrairAuthorities(claims);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(subject, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception e) {
            System.out.println("Erro JWT: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    private Collection<? extends GrantedAuthority> extrairAuthorities(Claims claims) {

        List<String> roles = extractListClaim(claims, "roles");
        if (roles == null || roles.isEmpty()) {
            roles = extractListClaim(claims, "authorities");
        }

        if (roles == null || roles.isEmpty()) {
            String role = claims.get("role", String.class);
            if (role != null && !role.trim().isEmpty()) {
                roles = List.of(role);
            }
        }

        if (roles == null) roles = Collections.emptyList();

        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(this::normalizarParaRoleAuthority)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<String> extractListClaim(Claims claims, String claimName) {
        Object claim = claims.get(claimName);
        if (claim instanceof List<?>) {
            return (List<String>) claim;
        }
        return Collections.emptyList();
    }

    private String normalizarParaRoleAuthority(String role) {
        if (role.startsWith("ROLE_")) return role;
        return "ROLE_" + role.toUpperCase(Locale.ROOT);
    }
}