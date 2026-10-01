package io.skystay.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public  class CorsConfig {
    @Value("S{app.cors.allowedOrigins}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration lgs = new CorsConfiguration();
        lgs.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        lgs.setAllowedMethods(List.of("POST", "GET", "PUT", "DELETE", "OPTIONS"));
        lgs.setAllowedHeaders(List.of("Autorization", "Content-type"));
        lgs.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", lgs);
        return  new CorsFilter(src);
    }
}