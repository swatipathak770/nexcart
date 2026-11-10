package com.nexcart.config;

import com.nexcart.security.JwtAuthenticationFilter;
import com.nexcart.security.CustomUserDetailsService;
import com.nexcart.security.JwtService;
import com.nexcart.security.ApiRateLimitFilter;
import com.nexcart.security.RequestIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService, CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http

                // =====================================
                // CORS
                // =====================================
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                // =====================================
                // CSRF
                // =====================================
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))

                .authorizeHttpRequests(auth -> auth

                        // =====================================
                        // CORS PREFLIGHT
                        // =====================================
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // =====================================
                        // Public APIs
                        // =====================================
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/webhooks/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // =====================================
                        // CUSTOMER APIs
                        // =====================================
                        .requestMatchers(
                                "/api/user/profile",
                                "/api/user/change-password",
                                "/api/cart/**",
                                "/api/wishlist/**",
                                "/api/addresses/**",
                                "/api/orders/**",
                                "/api/payments/**",
                                "/api/reviews/**"
                        ).hasRole("CUSTOMER")

                        // =====================================
                        // ADMIN APIs
                        // =====================================
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/user/users/**"
                        ).hasRole("ADMIN")

                        // =====================================
                        // CATEGORY APIs
                        // =====================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categories/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/categories/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/categories/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/categories/**"
                        ).hasRole("ADMIN")

                        // =====================================
                        // BRAND APIs
                        // =====================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/brands/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/brands/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/brands/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/brands/**"
                        ).hasRole("ADMIN")

                        // =====================================
                        // PRODUCT APIs
                        // =====================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/products/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/products/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/products/**"
                        ).hasRole("ADMIN")

                        // =====================================
                        // COUPON APIs
                        // =====================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/coupons/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/coupons"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/coupons/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/coupons/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/coupons/apply"
                        ).hasRole("CUSTOMER")

                        // =====================================
                        // Everything else
                        // =====================================
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(new RequestIdFilter(), JwtAuthenticationFilter.class)
                .addFilterAfter(new ApiRateLimitFilter(), JwtAuthenticationFilter.class);

        return http.build();
    }


    // =====================================
    // CORS CONFIGURATION
    // =====================================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.stream(
                System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS", "http://localhost:5173,http://localhost:3000").split(",")
        ).map(String::trim).filter(origin -> !origin.isEmpty()).toList());

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
