package sus.liva.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import sus.liva.application.port.in.NovoAtendimento;
import sus.liva.domain.model.agendamento.TipoAtendimento;

import java.time.LocalDateTime;

public record AgendamentoRequest(
        @Schema(description = "Nome completo do paciente", example = "Maria da Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        String nomePaciente,
        @Schema(description = "CPF do paciente (opcional). Se já existir, o paciente é reaproveitado", example = "52998224725")
        String cpf,
        @Schema(description = "Telefone do paciente com DDD", example = "51993953661", requiredMode = Schema.RequiredMode.REQUIRED)
        String telefone,
        @Schema(description = "Nome do cuidador (obrigatório se houver telefone do cuidador)", example = "João Cuidador")
        String nomeCuidador,
        @Schema(description = "Telefone do cuidador com DDD (opcional); o cuidador também recebe as mensagens")
        String telefoneCuidador,
        @Schema(description = "Data e hora do atendimento, no futuro", example = "2026-11-20T09:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dataHora,
        @Schema(description = "Tipo do atendimento", requiredMode = Schema.RequiredMode.REQUIRED)
        TipoAtendimento tipo,
        @Schema(description = "Unidade de saúde", example = "UBS Centro", requiredMode = Schema.RequiredMode.REQUIRED)
        String unidade,
        @Schema(description = "Endereço da unidade", example = "Rua das Flores, 123 - Centro", requiredMode = Schema.RequiredMode.REQUIRED)
        String endereco,
        @Schema(description = "Orientações de preparo (aparecem no lembrete da véspera)", example = "Jejum de 8 horas")
        String preparo
) {
    public NovoAtendimento paraNovoAtendimento() {
        return new NovoAtendimento(nomePaciente, cpf, telefone, nomeCuidador, telefoneCuidador, dataHora, tipo,
                unidade, endereco, preparo);
    }
}
