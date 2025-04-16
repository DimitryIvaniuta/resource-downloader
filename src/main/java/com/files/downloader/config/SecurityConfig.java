package com.files.downloader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since this is a REST API.
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to these endpoints.
                        .requestMatchers("/api/download-files", "/api/downloaded-files", "/api/auth/**").permitAll()
                        // If you have any other endpoints that you want to make public, add them here.
                        .anyRequest().permitAll()
                );
        return http.build();
    }

}
