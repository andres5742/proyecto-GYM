package com.gym.management.config;

import com.gym.management.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedback")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trainer-ratings/participants")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/trainer-ratings")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/access/verify")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/modules/public")
                        .permitAll()
                        .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/holidays/**",
                                        "/api/business-hours/**",
                                        "/api/wall-posts",
                                        "/api/home/carousel",
                                        "/api/home/media",
                                        "/api/home/footer")
                        .permitAll()
                        .requestMatchers("/api/feedback/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/trainer-ratings/monthly")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/trainer-ratings/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/access/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(
                                        "/api/holidays/**",
                                        "/api/business-hours/**",
                                        "/api/wall-posts/**",
                                        "/api/home/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/employees/active")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                        .requestMatchers("/api/employees/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/payroll-config/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/modules")
                        .authenticated()
                        .requestMatchers("/api/modules/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/members/**", "/api/plans/**", "/api/products/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
