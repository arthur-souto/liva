package sus.liva.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Liva - Lembretes de Agendamento")
                .version("0.0.1")
                .description("Lembretes e confirmação de agendamentos do SUS pelo WhatsApp (Evolution API). "
                        + "Para testar de ponta a ponta, comece por POST /api/teste/atendimentos."));
    }
}
