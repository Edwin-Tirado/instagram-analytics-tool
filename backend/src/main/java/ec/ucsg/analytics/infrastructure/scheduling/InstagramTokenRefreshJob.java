package ec.ucsg.analytics.infrastructure.scheduling;

import ec.ucsg.analytics.infrastructure.instagram.InstagramTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Renueva el access token de larga duración de Instagram antes de que expire
 * (dura 60 días; Meta exige que tenga al menos 24h de antigüedad para
 * renovarlo). Corre semanal — de sobra para nunca llegar al límite.
 *
 * Resiliencia: si la renovación falla (token ya expirado, error de red),
 * queda solo en el log — no debe tumbar el resto del backend. Si esto
 * ocurre, hay que generar un token nuevo manualmente (Instagram tester +
 * "Generar identificadores de acceso" en el panel de Meta).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramTokenRefreshJob {

    private final InstagramTokenProvider tokenProvider;

    @Scheduled(cron = "0 0 4 * * MON")
    public void refreshToken() {
        log.info("=== Inicio de renovación semanal del token de Instagram ===");
        try {
            tokenProvider.refreshToken();
            log.info("=== Token de Instagram renovado exitosamente ===");
        } catch (Exception e) {
            log.error("Fallo renovando el token de Instagram — habrá que generar uno nuevo manualmente si expira: {}",
                e.getMessage(), e);
        }
    }
}
