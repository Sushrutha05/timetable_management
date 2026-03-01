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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable global CORS support using our explicit configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF for stateless APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Disable default login forms
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Authorize Requests
                .authorizeHttpRequests(auth -> auth
                        // Explicitly allow all POST requests to our API endpoints
                        .requestMatchers(HttpMethod.POST, "/api/**").permitAll()
                        // Explicitly allow all GET requests to our API endpoints
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // Explicitly allow all PUT requests (updates/edits)
                        .requestMatchers(HttpMethod.PUT, "/api/**").permitAll()
                        // Explicitly allow all PATCH requests
                        .requestMatchers(HttpMethod.PATCH, "/api/**").permitAll()
                        // Explicitly allow all DELETE requests
                        .requestMatchers(HttpMethod.DELETE, "/api/**").permitAll()
                        // Any other request requires authentication
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Adding a PasswordEncoder bean is best practice and helps Spring Security
    // configure itself correctly, even if we aren't using it for login yet.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}