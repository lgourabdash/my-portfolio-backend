package com.lgourabdash.portfolio;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    /**
     * Comma-separated origin patterns for browser calls to this API (production frontend, preview
     * URLs, etc.). Env: {@code PORTFOLIO_CORS_ORIGIN_PATTERNS}
     */
    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${portfolio.cors.allowed-origin-patterns}") String allowedPatternsCsv) {
        String[] patterns =
                Arrays.stream(allowedPatternsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(patterns)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
