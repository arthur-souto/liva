package sus.liva.infrastructure.adapter.in.web.teste;

import io.swagger.v3.oas.annotations.media.Schema;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.time.LocalDateTime;

public record AtendimentoTesteRequest(
        @Schema(description = "Telefone do paciente com DDD", example = "51993953661", requiredMode = Schema.RequiredMode.REQUIRED)
        String telefone,
        @Schema(description = "Nome do paciente", example = "Paciente Teste", defaultValue = "Paciente Teste")
        String nomePaciente,
        @Schema(description = "Nome do cuidador (opcional)", example = "João Cuidador")
        String nomeCuidador,
        @Schema(description = "Telefone do cuidador com DDD (opcional); se informado, o cuidador também recebe os lembretes")
        String telefoneCuidador,
        @Schema(description = "Data e hora do atendimento; padrão: daqui a 2 dias às 09:30", example = "2026-11-20T09:30:00")
        LocalDateTime dataHora,
        @Schema(description = "Tipo do atendimento", defaultValue = "CONSULTA")
        TipoAtendimento tipo,
        @Schema(description = "Unidade de saúde", defaultValue = "UBS Centro")
        String unidade,
        @Schema(description = "Endereço da unidade", defaultValue = "Rua das Flores, 123 - Centro")
        String endereco,
        @Schema(description = "Orientações de preparo (aparecem no lembrete da véspera)", example = "Jejum de 8 horas")
        String preparo,
        @Schema(description = "Lembrete enviado logo após criar o atendimento; envie disparar=false para não enviar nada", defaultValue = "PEDIDO_CONFIRMACAO")
        TipoLembrete lembrete,
        @Schema(description = "Se deve disparar o lembrete imediatamente", defaultValue = "true")
        Boolean disparar
) {}
