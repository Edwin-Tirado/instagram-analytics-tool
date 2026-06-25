package ec.ucsg.analytics.infrastructure.security;

/**
 * NOTA: La configuración CORS ya está declarada dentro de SecurityConfig.java
 * (método corsConfigurationSource()) y cubre http://localhost:3000 (Next.js dev)
 * y http://localhost:3001.
 *
 * Este archivo existe solo como referencia independiente. NO añadir @Configuration
 * aquí — duplicar el CorsConfigurationSource generaría un conflicto de beans.
 *
 * Para agregar orígenes adicionales en producción, edita SecurityConfig:
 *
 *   config.setAllowedOrigins(List.of(
 *       "http://localhost:3000",
 *       "https://eventos.ucsg.edu.ec"   ← dominio producción
 *   ));
 */
public final class CorsConfig {
  private CorsConfig() {}
}
