package sus.liva.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sus.liva.infrastructure.adapter.in.web.seguranca.ChaveApiFiltro;

@Configuration
public class OpenApiConfig {
    private static final String ESQUEMA = "chaveApi";
    private static final String ESQUEMA_JWT = "sessao";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Liva - Lembretes de Agendamento")
                        .version("0.0.1")
                        .description("Lembretes e confirmação de agendamentos do SUS pelo WhatsApp (Evolution API). "
                                + "Rotas de sistema aceitam a API_KEY do .env; as rotas do fluxo assistencial exigem o token "
                                + "obtido em POST /api/auth/entrar (Bearer). "
                                + "Para testar de ponta a ponta, comece por POST /api/teste/atendimentos."))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(ChaveApiFiltro.CABECALHO))
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
