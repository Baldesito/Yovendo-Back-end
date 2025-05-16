package com.example.demo.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.demo.payloadDTO.request.LoginRequest;
import com.example.demo.security.model.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private final AuthenticationManager authenticationManager;
    private final String jwtSecret;
    private final long jwtExpiration;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, String jwtSecret, long jwtExpiration) {
        this.authenticationManager = authenticationManager;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
        setFilterProcessesUrl("/api/auth/login"); // Imposta l'URL per il login
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        LoginRequest credentials;

        // Determina il tipo di contenuto della richiesta
        String contentType = request.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            // Leggi dati JSON dal body
            try {
                credentials = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
            } catch (Exception e) {
                logger.error("Errore nella lettura del JSON dalla richiesta", e);
                throw new RuntimeException("Formato JSON non valido", e);
            }
        } else {
            // Leggi dati da form parameters
            String email = request.getParameter("email");
            String password = request.getParameter("password");

            // Se i parametri sono ancora null, prova a verificare se è un form vuoto
            if (email == null || password == null) {
                try {
                    // Ultimo tentativo di leggere dal body come JSON
                    String body = request.getReader().lines().collect(Collectors.joining());
                    if (body != null && !body.isEmpty()) {
                        credentials = new ObjectMapper().readValue(body, LoginRequest.class);
                    } else {
                        throw new RuntimeException("Dati di autenticazione mancanti");
                    }
                } catch (Exception e) {
                    logger.error("Nessun dato di autenticazione trovato nella richiesta", e);
                    throw new RuntimeException("Email e password richiesti", e);
                }
            } else {
                // Crea un oggetto LoginRequest dai parametri del form
                credentials = new LoginRequest();
                credentials.setEmail(email);
                credentials.setPassword(password);
            }
        }

        logger.info("Tentativo di autenticazione per email: {}", credentials.getEmail());

        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword())
        );
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authResult.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        logger.info("Autenticazione riuscita per l'utente email: {}", userDetails.getEmail());

        String token = JWT.create()
                .withSubject(userDetails.getEmail())  // Usa email come subject
                .withClaim("id", userDetails.getId())
                .withClaim("email", userDetails.getEmail())
                .withClaim("username", userDetails.getRealUsername())  // Salva anche username
                .withClaim("roles", roles)
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration))
                .sign(Algorithm.HMAC256(jwtSecret.getBytes()));

        // Costruisci una risposta JSON completa
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", userDetails.getId());
        responseBody.put("username", userDetails.getRealUsername());
        responseBody.put("email", userDetails.getEmail());
        responseBody.put("role", roles.isEmpty() ? "ROLE_USER" : roles.get(0));
        responseBody.put("message", "Login effettuato con successo");
        responseBody.put("token", token);

        response.setContentType("application/json");
        new ObjectMapper().writeValue(response.getOutputStream(), responseBody);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {
        logger.error("Autenticazione fallita: {}", failed.getMessage());
        failed.printStackTrace();  // Aggiungi lo stack trace per debugging

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "Autenticazione fallita: " + failed.getMessage());

        response.setContentType("application/json");
        new ObjectMapper().writeValue(response.getOutputStream(), responseBody);
    }
}