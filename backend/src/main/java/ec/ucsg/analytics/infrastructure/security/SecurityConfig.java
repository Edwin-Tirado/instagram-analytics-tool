package ec.ucsg.analytics.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * Arquitectura: Stateless JWT — no se usan sesiones HTTP.
 *
 * Orden de filtros:
 *   1. RateLimitingFilter      → bloquea IPs abusivas antes de cualquier lógica
 *   2. JwtAuthenticationFilter → extrae identidad del Bearer token
 *   3. UsernamePasswordAuthenticationFilter (Spring default, no se usa en modo JWT)
 *
 * Matriz de acceso por endpoint:
 * ┌─────────────────────────────────┬──────────────────────────┐
 * │ Endpoint                        │ Acceso                   │
 * ├─────────────────────────────────┼──────────────────────────┤
 * │ POST /api/auth/**               │ Público                  │
 * │ GET  /api/public/**             │ Público                  │
 * │ GET  /api/events/map            │ Público                  │
 * │ PUT  /api/events/*/approve      │ SUPERVISOR, ADMIN        │
 * │ PUT  /api/events/*/reject       │ SUPERVISOR, ADMIN        │
 * │ GET  /api/supervisor/**         │ SUPERVISOR, ADMIN        │
 * │ /**  /api/admin/**              │ ADMIN                    │
 * │ Cualquier otro                  │ Autenticado              │
 * └─────────────────────────────────┴──────────────────────────┘
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl  userDetailsService;
    private final RateLimitingFilter      rateLimitingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Beans de infraestructura ────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Oculta UsernameNotFoundException — no revela si el email existe
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Cadena de filtros principal ─────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // Sin estado — JWT en Authorization header
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CSRF innecesario con JWT stateless
            .csrf(AbstractHttpConfigurer::disable)

            // CORS — orígenes configurados en CorsConfigurationSource
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Reglas de autorización por ruta
            .authorizeHttpRequests(auth -> auth

                // Registro, login y refresh — públicos (el refresh valida el token internamente)
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()

                // Mapa y listado de eventos aprobados — públicos (consumidos por el mapa)
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events/map").permitAll()

                // Actuator — solo localhost (se refina con IpAddressRequestMatcher en prod)
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Gestión completa del sistema
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Revisión de posts pendientes
                .requestMatchers("/api/supervisor/**").hasAnyRole("SUPERVISOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/events/*/approve").hasAnyRole("SUPERVISOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/events/*/reject").hasAnyRole("SUPERVISOR", "ADMIN")

                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )

            // Proveedor de autenticación personalizado
            .authenticationProvider(authenticationProvider())

            // JWT valida identidad antes del filtro de usuario/contraseña
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Rate Limiting se ejecuta antes del filtro JWT
            .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    // ── CORS ────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // En producción, reemplazar por el dominio del frontend desplegado
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",   // Next.js dev
            "http://localhost:3001"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
