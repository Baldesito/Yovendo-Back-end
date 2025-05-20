package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Consenti specifici origini
        config.addAllowedOrigin("http://localhost:5173"); // Vite dev
        config.addAllowedOrigin("http://localhost:4173"); // Vite preview
        config.addAllowedOrigin("https://yovendo-ai.netlify.app"); // Frontend su Netlify

        // Aggiungi altri header necessari
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight per 1 ora

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}