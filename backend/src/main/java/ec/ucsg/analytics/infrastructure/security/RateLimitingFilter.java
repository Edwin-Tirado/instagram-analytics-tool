package ec.ucsg.analytics.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de Rate Limiting por IP usando Bucket4j (token bucket).
 *
 * Política:
 *   - Endpoints de auth (/api/auth/**): 10 req/min por IP.
 *   - Endpoints públicos   (/api/public/**): 60 req/min por IP.
 *
 * El mapa de buckets es in-memory. En producción con múltiples instancias,
 * reemplazar por Bucket4j + Redis o Hazelcast para estado distribuido.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int AUTH_CAPACITY       = 10;
    private static final int PUBLIC_CAPACITY      = 60;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    private final Map<String, Bucket> authBuckets   = new ConcurrentHashMap<>();
    private final Map<String, Bucket> publicBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/")) {
            Bucket bucket = authBuckets.computeIfAbsent(
                getClientIp(request), ip -> buildBucket(AUTH_CAPACITY)
            );
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit superado en /api/auth/ para IP: {}", getClientIp(request));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intente más tarde.\"}");
                return;
            }
        } else if (path.startsWith("/api/public/")) {
            Bucket bucket = publicBuckets.computeIfAbsent(
                getClientIp(request), ip -> buildBucket(PUBLIC_CAPACITY)
            );
            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intente más tarde.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket buildBucket(int capacity) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, REFILL_DURATION)
                .build())
            .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
