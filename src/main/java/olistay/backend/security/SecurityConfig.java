package olistay.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * Session policy is STATELESS — the server holds no session state for
 * authentication; every request must carry a valid JWT access token. Refresh
 * tokens are handled outside this filter chain entirely (httpOnly cookie,
 * read directly by AuthController), which is why /auth/refresh is permitted
 * here without requiring an access token.
 *
 * CORS is configured to allow credentials, which is required for the httpOnly
 * refresh cookie to be sent by the browser on cross-origin requests from the
 * React frontend.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize("hasRole('ADMIN')") etc. on controllers/services
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // not needed: stateless JWT auth, no cookie-based session auth
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh"
                        ).permitAll()
                        .requestMatchers("/auth/logout").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/host/**").hasAnyRole("HOST", "ADMIN")
                        // Public property browsing — order matters: specific GET
                        // matchers must come before the catch-all POST/PUT/DELETE
                        // on the same base path, AND literal paths must be
                        // registered before the {id} wildcard below, since
                        // "/properties/recommendations" would otherwise match
                        // the {id} pattern (with id="recommendations") and
                        // incorrectly become public.
                        .requestMatchers(HttpMethod.GET, "/properties").permitAll()
                        .requestMatchers(HttpMethod.GET, "/properties/city/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/properties/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/properties/recommendations").authenticated()
                        .requestMatchers(HttpMethod.GET, "/properties/{id}/score").authenticated()
                        .requestMatchers(HttpMethod.GET, "/properties/{id}").permitAll()
                        // Everything else under /properties (create, update, delete,
                        // status changes, /me, /{id}/details) requires authentication;
                        // ownership/role checks happen in PropertyServiceImpl.
                        .requestMatchers("/properties/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/{id}").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Allows the configured frontend origin to send credentialed requests
     * (required so the browser includes the httpOnly refresh cookie).
     * allowedOrigins is intentionally explicit rather than "*" — wildcard
     * origins are incompatible with allowCredentials(true) per the CORS spec.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}