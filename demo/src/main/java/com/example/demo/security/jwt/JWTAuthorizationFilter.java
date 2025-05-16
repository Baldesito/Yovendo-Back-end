package com.example.demo.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthorizationFilter.class);

    private final String jwtSecret;
    private final UserDetailsService userDetailsService;

    public JWTAuthorizationFilter(AuthenticationManager authenticationManager, String jwtSecret, UserDetailsService userDetailsService) {
        super(authenticationManager);
        this.jwtSecret = jwtSecret;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication authentication = getAuthentication(request);

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

    private Authentication getAuthentication(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token != null) {
            try {
                // Rimuovi il prefisso "Bearer "
                token = token.replace("Bearer ", "");

                // Decodifica e verifica il token
                DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(jwtSecret.getBytes()))
                        .build()
                        .verify(token);

                // Estrai l'email dal token (ora il subject è l'email)
                String email = decodedJWT.getSubject();

                logger.debug("Autenticazione token JWT per email: {}", email);

                if (email != null) {
                    // Carica i dettagli dell'utente usando l'email
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // Crea un'autenticazione
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                }
            } catch (Exception e) {
                logger.error("Errore nella verifica del token: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }
}