package ec.ucsg.analytics.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio de tokens JWT usando JJWT 0.12.x.
 *
 * Genera dos tipos de token:
 *   - Access Token  (corta duración, lleva los roles en el claim "roles").
 *   - Refresh Token (larga duración, sin roles — solo identidad).
 *
 * La firma usa HMAC-SHA256. El secreto DEBE ser ≥ 256 bits (32 bytes)
 * y cargarse desde la variable de entorno JWT_SECRET, nunca hardcodeado.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_ROLES      = "roles";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TYPE_ACCESS      = "access";
    private static final String TYPE_REFRESH     = "refresh";

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Generación ──────────────────────────────────────────────────

    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        return buildToken(
            userDetails.getUsername(),
            Map.of(CLAIM_ROLES, roles, CLAIM_TOKEN_TYPE, TYPE_ACCESS),
            accessExpirationMs
        );
    }

    public String generateAccessToken(String email, Collection<? extends GrantedAuthority> authorities) {
        List<String> roles = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        return buildToken(
            email,
            Map.of(CLAIM_ROLES, roles, CLAIM_TOKEN_TYPE, TYPE_ACCESS),
            accessExpirationMs
        );
    }

    public String generateRefreshToken(String email) {
        return buildToken(
            email,
            Map.of(CLAIM_TOKEN_TYPE, TYPE_REFRESH),
            refreshExpirationMs
        );
    }

    private String buildToken(String subject, Map<String, Object> extraClaims, long expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
            .claims(extraClaims)
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMs)))
            .signWith(getSigningKey())
            .compact();
    }

    // ── Extracción de claims ────────────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get(CLAIM_ROLES));
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    // ── Validación ──────────────────────────────────────────────────

    /**
     * Valida firma, expiración y que el token sea de tipo "access".
     * Lanza {@link JwtException} con mensaje descriptivo ante cualquier anomalía.
     */
    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        try {
            String email     = extractEmail(token);
            String tokenType = extractClaim(token, c -> c.get(CLAIM_TOKEN_TYPE, String.class));

            boolean emailMatches     = email.equals(userDetails.getUsername());
            boolean notExpired       = !isTokenExpired(token);
            boolean isAccessType     = TYPE_ACCESS.equals(tokenType);

            return emailMatches && notExpired && isAccessType;

        } catch (ExpiredJwtException e) {
            log.debug("JWT expirado: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Firma JWT inválida");
            return false;
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("JWT malformado o inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, String email) {
        try {
            String subject   = extractEmail(token);
            String tokenType = extractClaim(token, c -> c.get(CLAIM_TOKEN_TYPE, String.class));
            return subject.equals(email)
                && !isTokenExpired(token)
                && TYPE_REFRESH.equals(tokenType);
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // ── Clave de firma ──────────────────────────────────────────────

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
