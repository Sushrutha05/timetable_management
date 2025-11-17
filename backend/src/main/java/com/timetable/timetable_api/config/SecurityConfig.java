package com.timetable.timetable_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
                        // --- UPDATE THIS SECTION ---
                        // Explicitly allow all POST requests to our API endpoints
                        .requestMatchers(HttpMethod.POST, "/api/**").permitAll()
                        // Explicitly allow all GET requests to our API endpoints
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // Explicitly allow all DELETE requests
                        .requestMatchers(HttpMethod.DELETE, "/api/**").permitAll()
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // --- ADD THIS BEAN ---
    // Adding a PasswordEncoder bean is best practice and helps Spring Security
    // configure itself correctly, even if we aren't using it for login yet.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}