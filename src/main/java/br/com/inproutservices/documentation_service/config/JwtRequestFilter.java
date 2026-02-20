package br.com.inproutservices.documentation_service.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
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
            byte[] secretBytes = Base64.getDecoder().decode(secret);
            Algorithm algorithm = Algorithm.HMAC256(secretBytes);

            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);

            String subject = jwt.getSubject();

            if (subject != null && !subject.trim().isEmpty()) {

                Collection<? extends GrantedAuthority> authorities = extrairAuthorities(jwt);

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

    private Collection<? extends GrantedAuthority> extrairAuthorities(DecodedJWT jwt) {

        List<String> roles = jwt.getClaim("roles").asList(String.class);
        if (roles == null || roles.isEmpty()) {
            roles = jwt.getClaim("authorities").asList(String.class);
        }

        if (roles == null || roles.isEmpty()) {
            String role = jwt.getClaim("role").asString();
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

    private String normalizarParaRoleAuthority(String role) {
        if (role.startsWith("ROLE_")) return role;
        return "ROLE_" + role.toUpperCase(Locale.ROOT);
    }
}