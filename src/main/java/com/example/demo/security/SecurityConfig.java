package com.example.demo.security;

import com.example.demo.repository.UtenteRepository;
import com.example.demo.security.jwt.JWTAuthenticationFilter;
import com.example.demo.security.jwt.JWTAuthorizationFilter;
import com.example.demo.service.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    UtenteRepository utenteRepository;

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        logger.info("Configurazione di DaoAuthenticationProvider con UserDetailsService: {}",
                userDetailsService.getClass().getName());

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false);

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:4173",
                "https://yovendo-ai.netlify.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Otteniamo l'AuthenticationManager dal contesto
        AuthenticationManager authenticationManager = authenticationManager(http.getSharedObject(AuthenticationConfiguration.class));

        // Configura i filtri JWT con l'AuthenticationManager
        JWTAuthenticationFilter jwtAuthenticationFilter = new JWTAuthenticationFilter(
                authenticationManager, jwtSecret, jwtExpiration);
        jwtAuthenticationFilter.setFilterProcessesUrl("/api/auth/login");

        JWTAuthorizationFilter jwtAuthorizationFilter = new JWTAuthorizationFilter(
                authenticationManager, jwtSecret, userDetailsService);

        http
                // Abilita CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disabilita CSRF per API stateless
                .csrf(AbstractHttpConfigurer::disable)

                // Imposta sessione come stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configura autorizzazioni degli endpoint
                .authorizeHttpRequests(auth -> auth
                        // Endpoint di autenticazione pubblici
                        .requestMatchers("/api/auth/**").permitAll()

                        // Endpoint health check
                        .requestMatchers("/api/health", "/").permitAll()

                        .requestMatchers("/api/public/**").permitAll()

                        // Endpoint webhook
                        .requestMatchers("/webhook/**", "/api/webhook/**").permitAll()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Endpoint utenti
                        .requestMatchers("/api/utenti/register", "/api/utenti/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/utenti/**").permitAll()

                        // API endpoints in sola lettura
                        .requestMatchers(HttpMethod.GET, "/api/organizzazioni/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/conversazioni/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/documenti/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/statistiche/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/conversazioni/*/chiudi").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/conversazioni/*/riapri").permitAll()

                        // Endpoint per upload documenti
                        .requestMatchers(HttpMethod.POST, "/api/documenti").permitAll()

                        // Risorse statiche
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // API protette di default
                        .anyRequest().authenticated()
                )
                // Aggiungi filtri JWT
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Aggiungi provider di autenticazione
        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}