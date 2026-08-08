package com.benhsoan.config;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .csrf(AbstractHttpConfigurer::disable)

                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                // ===== PUBLIC =====
                                                .requestMatchers("/auth/**").permitAll()

                                                .requestMatchers(
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/swagger-resources/**",
                                                                "/webjars/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/actuator/health",
                                                                "/actuator/info")
                                                .permitAll()

                                                // ===== ADMIN / USER MANAGEMENT =====
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/users/doctors")
                                                .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                                                .requestMatchers("/users/**").hasRole("ADMIN")
                                                .requestMatchers("/audit-logs/**").hasRole("ADMIN")
                                                .requestMatchers("/roles/**").hasRole("ADMIN")
                                                .requestMatchers("/permissions/**").hasRole("ADMIN")

                                                // ===== PATIENTS =====
                                                .requestMatchers(HttpMethod.GET, "/patients/me/**")
                                                .hasAnyRole("DOCTOR", "NURSE", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.GET, "/patients/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.POST, "/patients/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.PUT, "/patients/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.DELETE, "/patients/**").hasRole("ADMIN")

                                                // ===== MEDICAL RECORDS =====
                                                .requestMatchers(HttpMethod.GET, "/medical-history/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.GET, "/visits/*/encounter")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.POST, "/medical-records/*/lock")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.GET, "/medical-records/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.POST, "/medical-records/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.PUT, "/medical-records/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.DELETE, "/medical-records/**").hasRole("ADMIN")

                                                // ===== PRESCRIPTIONS / CLINICAL =====
                                                .requestMatchers(HttpMethod.POST, "/prescriptions/*/dispense")
                                                .hasAnyRole("ADMIN", "PHARMACIST")
                                                .requestMatchers(HttpMethod.GET, "/prescriptions/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                                                .requestMatchers(HttpMethod.POST, "/prescriptions/**")
                                                .hasRole("DOCTOR")
                                                .requestMatchers(HttpMethod.PATCH, "/prescriptions/**")
                                                .hasRole("DOCTOR")
                                                .requestMatchers(HttpMethod.PUT, "/prescriptions/**")
                                                .hasRole("DOCTOR")
                                                .requestMatchers(HttpMethod.DELETE, "/prescriptions/**")
                                                .hasRole("DOCTOR")
                                                .requestMatchers(HttpMethod.GET, "/clinical-services/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.GET, "/clinical-orders/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.POST, "/clinical-orders/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.POST, "/clinical-order-items/*/results")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.POST, "/clinical-results/*/finalize")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.POST, "/clinical-results/*/attachments")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.PUT, "/clinical-results/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.GET, "/clinical-results/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.GET, "/clinical-result-attachments/*/download")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.GET, "/diagnosis-catalog/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers("/vital-signs/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                                                // ===== APPOINTMENTS =====
                                                .requestMatchers(HttpMethod.POST, "/appointments/*/check-in")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.GET, "/appointments/me/**")
                                                .hasAnyRole("DOCTOR", "NURSE", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.PATCH, "/appointments/*/cancel")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.PATCH, "/appointments/*/no-show")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.POST, "/appointments/*/reminder")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.GET, "/appointments/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.POST, "/appointments/**")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.PUT, "/appointments/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.DELETE, "/appointments/**")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")

                                                // ===== PHARMACY / INVOICES =====
                                                .requestMatchers(HttpMethod.DELETE, "/inventory/**").hasRole("ADMIN")
                                                .requestMatchers("/inventory/**")
                                                .hasAnyRole("ADMIN", "PHARMACIST")
                                                .requestMatchers("/invoices/**")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")

                                                // ===== MEDICAL QUEUE =====
                                                .requestMatchers(HttpMethod.GET, "/queues/me")
                                                .hasRole("DOCTOR")
                                                .requestMatchers(HttpMethod.GET, "/queues")
                                                .hasAnyRole("ADMIN", "NURSE", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.POST, "/queues/*/call-next")
                                                .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.POST, "/queue-items/walk-in")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.POST, "/queue-items/*/complete")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.POST, "/queue-items/*/skip")
                                                .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.PATCH, "/queue-items/*/status")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.GET, "/queue-items/*")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")

                                                // ===== ROOMS =====
                                                .requestMatchers(HttpMethod.GET, "/rooms", "/rooms/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                                                .requestMatchers("/rooms", "/rooms/**").hasRole("ADMIN")
                                                .requestMatchers("/doctor-room-assignments/**").hasRole("ADMIN")
                                                .requestMatchers("/doctors/*/room-assignment").hasRole("ADMIN")

                                                // ===== OTHERS =====
                                                .anyRequest().authenticated())

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(authenticationEntryPoint())
                                                .accessDeniedHandler(accessDeniedHandler()))

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(List.of(
                                "http://localhost:3000",
                                "http://localhost:5173",
                                "http://localhost:4200"));

                configuration.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"));

                configuration.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));

                configuration.setExposedHeaders(List.of(
                                "Authorization",
                                "Content-Disposition"));

                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration authenticationConfiguration) throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public AuthenticationEntryPoint authenticationEntryPoint() {

                return (request, response, authException) -> {

                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");

                        ObjectMapper mapper = new ObjectMapper();

                        response.getWriter().write(
                                        mapper.writeValueAsString(Map.of(
                                                        "timestamp", Instant.now().toString(),
                                                        "status", HttpStatus.UNAUTHORIZED.value(),
                                                        "error", "Unauthorized",
                                                        "message", "Bạn cần đăng nhập để truy cập tài nguyên này",
                                                        "path", request.getRequestURI())));
                };
        }

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {

                return (request, response, accessDeniedException) -> {

                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");

                        ObjectMapper mapper = new ObjectMapper();

                        response.getWriter().write(
                                        mapper.writeValueAsString(Map.of(
                                                        "timestamp", Instant.now().toString(),
                                                        "status", HttpStatus.FORBIDDEN.value(),
                                                        "error", "Forbidden",
                                                        "message", "Bạn không có quyền truy cập tài nguyên này",
                                                        "path", request.getRequestURI())));
                };
        }
}
