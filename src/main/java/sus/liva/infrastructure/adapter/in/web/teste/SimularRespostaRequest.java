package sus.liva.infrastructure.adapter.in.web.teste;

import io.swagger.v3.oas.annotations.media.Schema;

public record SimularRespostaRequest(
        @Schema(description = "Telefone que está respondendo, com DDD", example = "51993953661", requiredMode = Schema.RequiredMode.REQUIRED)
        String telefone,
        @Schema(description = "Texto da resposta: 1/sim, 2/não, 3/outro horário", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        String texto
) {}
