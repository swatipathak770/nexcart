package com.nexcart.config;

import com.nexcart.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // =====================================
                        // Public APIs
                        // =====================================
                        .requestMatchers(
                                "/api/auth/**",
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
                                "/api/address/**",
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
                        .requestMatchers(HttpMethod.GET, "/api/categories/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/categories/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/categories/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**")
                        .hasRole("ADMIN")

                        // =====================================
                        // BRAND APIs
                        // =====================================
                        .requestMatchers(HttpMethod.GET, "/api/brands/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/brands/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/brands/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/brands/**")
                        .hasRole("ADMIN")

                        // =====================================
                        // PRODUCT APIs
                        // =====================================
                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/products/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasRole("ADMIN")

                        // =====================================
                        // COUPON APIs
                        // =====================================
                        .requestMatchers(HttpMethod.GET, "/api/coupons/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/coupons")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/coupons/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/coupons/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/coupons/apply")
                        .hasRole("CUSTOMER")

                        // =====================================
                        // Everything else
                        // =====================================
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}


