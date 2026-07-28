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

                                                // ===== MEDICAL HISTORY =====
                                                .requestMatchers(HttpMethod.GET, "/patients/*/medical-history")
                                                .hasAnyRole("ADMIN", "DOCTOR")

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
                                                .requestMatchers(HttpMethod.PATCH, "/medical-records/*/status")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.GET, "/medical-records/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.POST, "/medical-records/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.PUT, "/medical-records/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.DELETE, "/medical-records/**").hasRole("ADMIN")

                                                // ===== PRESCRIPTIONS / CLINICAL =====
                                                .requestMatchers(HttpMethod.PUT, "/prescriptions/*/status")
                                                .hasAnyRole("ADMIN", "PHARMACIST")
                                                .requestMatchers(HttpMethod.GET, "/prescriptions/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                                                .requestMatchers(HttpMethod.POST, "/prescriptions/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.PUT, "/prescriptions/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.DELETE, "/prescriptions/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers("/diagnoses/**").hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers("/vital-signs/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                                                // ===== APPOINTMENTS =====
                                                .requestMatchers(HttpMethod.GET, "/appointments/me/**")
                                                .hasAnyRole("DOCTOR", "NURSE", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.PATCH, "/appointments/*/cancel")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")
                                                .requestMatchers(HttpMethod.PATCH, "/appointments/no-show/**")
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
                                                .requestMatchers(HttpMethod.DELETE, "/pharmacy/inventory/**").hasRole("ADMIN")
                                                .requestMatchers("/pharmacy/inventory/**")
                                                .hasAnyRole("ADMIN", "PHARMACIST")
                                                .requestMatchers("/invoices/**")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")

                                                // ===== MEDICAL QUEUE =====
                                                .requestMatchers(HttpMethod.POST, "/queue/call-next")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.PUT, "/queue/*/status")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                                                .requestMatchers(HttpMethod.GET, "/queue/doctor/**")
                                                .hasAnyRole("ADMIN", "DOCTOR")
                                                .requestMatchers(HttpMethod.GET, "/queue/room/**")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST", "PHARMACIST")
                                                .requestMatchers(HttpMethod.GET, "/queue/count")
                                                .hasAnyRole("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST", "PHARMACIST")
                                                .requestMatchers(HttpMethod.POST, "/queue/**")
                                                .hasAnyRole("ADMIN", "RECEPTIONIST")

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
