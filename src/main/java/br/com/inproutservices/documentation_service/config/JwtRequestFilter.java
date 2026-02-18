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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64; // Importante
import java.util.Collections;

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

        String token = header.replace("Bearer ", "");

        try {
            byte[] secretBytes = Base64.getDecoder().decode(secret);
            Algorithm algorithm = Algorithm.HMAC256(secretBytes);

            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);

            String usuario = jwt.getSubject();

            if (usuario != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception e) {

            System.out.println("Erro JWT: " + e.getMessage()); // Log útil para debug no docker logs
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}