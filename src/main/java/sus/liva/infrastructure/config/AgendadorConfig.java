package sus.liva.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "confirmasus.disparo-diario.habilitado", havingValue = "true", matchIfMissing = true)
public class AgendadorConfig {
}
