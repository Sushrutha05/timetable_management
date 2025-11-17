package com.timetable.timetable_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Disable default login forms
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Authorize Requests
                .authorizeHttpRequests(auth -> auth
                        // Explicitly allow all requests to our API endpoints
                        .requestMatchers("/api/**").permitAll()
                        // Any other request requires authentication (good practice to keep)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}