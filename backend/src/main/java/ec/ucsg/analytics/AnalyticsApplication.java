package ec.ucsg.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableCaching
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
