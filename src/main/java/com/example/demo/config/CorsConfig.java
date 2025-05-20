package com.example.demo.config;

import org.apache.catalina.filters.CorsFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173,https://yovendo-ai.netlify.app,https://yovendo-ai.onrender.com}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Consenti tutti gli origini in ambiente di sviluppo
        // In produzione, dovresti limitare solo ai tuoi domini frontend
        config.addAllowedOrigin("http://localhost:5173"); // Vite dev server
        config.addAllowedOrigin("http://localhost:4173"); // Vite preview
        config.addAllowedOrigin("https://yovendo-ai.netlify.app"); // Frontend production

        // Uncomment per test diretti al backend
        // config.addAllowedOrigin("*");

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true); // Importante per le richieste autenticate

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter();
    }
}