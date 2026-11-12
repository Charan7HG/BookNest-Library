package com.pentagon.LibraryManagement.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private PasswordChangeRequiredFilter passwordChangeRequiredFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // No anonymous identity, so a missing login is 401 rather than 403.
            .anonymous(anonymous -> anonymous.disable())

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN)))

            .authorizeHttpRequests(auth -> auth
                // Login and password-reset request submission are public. The old
                // email-only reset endpoint is permanently disabled because it let
                // anyone overwrite a password.
                .requestMatchers(HttpMethod.POST, "/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/forgot-password").denyAll()

                // Authenticated password change. Must stay reachable while a member
                // still has a temporary password.
                .requestMatchers(HttpMethod.POST, "/change-password").authenticated()

                // Public: a member submits a password reset request. The response is
                // always generic, so this reveals nothing about registered emails.
                .requestMatchers(HttpMethod.POST, "/password-reset-requests").permitAll()

                // ADMIN / LIBRARIAN: review password reset requests. MEMBER accounts
                // can neither list nor process them.
                .requestMatchers(HttpMethod.GET, "/password-reset-requests",
                        "/password-reset-requests/pending")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.POST, "/password-reset-requests/*/approve",
                        "/password-reset-requests/*/reject")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")

                // Every other user management route, including PUT and DELETE /user/{id}, is ADMIN-only.
                .requestMatchers("/user", "/users", "/user/**").hasAuthority("ROLE_ADMIN")

                // ADMIN-only: view all profiles
                .requestMatchers("/getall").hasAuthority("ROLE_ADMIN")

                // All three roles can access own profile functionality (ownership checks in Phase 2C.3)
                .requestMatchers("/saveprofile").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")

                // Book operations - READ: all three roles
                .requestMatchers(HttpMethod.GET, "/books").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/book/{id}").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/book/search").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")

                // Book operations - WRITE: ADMIN and LIBRARIAN only
                .requestMatchers(HttpMethod.POST, "/book").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.PUT, "/book/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.DELETE, "/book/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")

                // Legacy /bookbr is kept for compatibility but always answers 410 GONE.
                .requestMatchers(HttpMethod.GET, "/bookbr").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")

                // Borrowing workflow: only MEMBERs submit and view their own requests.
                .requestMatchers(HttpMethod.POST, "/borrow-requests").hasAuthority("ROLE_MEMBER")
                .requestMatchers(HttpMethod.GET, "/borrow-requests/my").hasAuthority("ROLE_MEMBER")

                // Reviewing requests, listing active loans, and recording returns
                // are staff-only operations.
                .requestMatchers(HttpMethod.GET, "/borrow-requests/pending")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.POST, "/borrow-requests/*/approve", "/borrow-requests/*/reject")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.GET, "/loans/active")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.POST, "/loans/*/return")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")

                // Author operations - READ: all three roles
                .requestMatchers(HttpMethod.GET, "/authors").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/author/{id}").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/author/search").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/author/books/{id}").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")

                // Author operations - WRITE: ADMIN and LIBRARIAN only
                .requestMatchers(HttpMethod.POST, "/author").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.PUT, "/author/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.DELETE, "/author/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")

                // Category operations - READ: all three roles
                .requestMatchers(HttpMethod.GET, "/categories").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/category/{id}").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/category/search").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/category/books/{id}").hasAnyAuthority("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")

                // Category operations - WRITE: ADMIN and LIBRARIAN only
                .requestMatchers(HttpMethod.POST, "/category").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.PUT, "/category/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")
                .requestMatchers(HttpMethod.DELETE, "/category/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")

                // Dashboard accessible to all authenticated roles
                .requestMatchers("/dashboard").authenticated()

                // Default: any other request requires authentication
                .anyRequest().authenticated()
            )

            .httpBasic(httpBasic -> httpBasic.disable())

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(passwordChangeRequiredFilter, JwtFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);
        configuration.addAllowedOrigin(allowedOrigin);
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
