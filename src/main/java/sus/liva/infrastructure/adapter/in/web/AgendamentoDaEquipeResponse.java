package sus.liva.infrastructure.adapter.in.web;

import sus.liva.application.port.in.AgendamentoDetalhado;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.paciente.Paciente;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoDaEquipeResponse(
        UUID id,
        LocalDateTime dataHora,
        TipoAtendimento tipo,
        String unidade,
        StatusAgendamento status,
        UUID pacienteId,
        String nomePaciente,
        String telefone,
        String nomeCuidador,
        String telefoneCuidador
) {
    public static AgendamentoDaEquipeResponse de(AgendamentoDetalhado detalhado) {
        Agendamento a = detalhado.agendamento();
        Paciente p = detalhado.paciente();

        return new AgendamentoDaEquipeResponse(a.getId(), a.getDataHora(), a.getTipo(), a.getUnidade(), a.getStatus(),
                a.getPacienteId(),
                p == null ? null : p.nomeCompleto(),
                p == null ? null : digitos(p.telefone()),
                p == null ? null : p.nomeCuidador(),
                p == null ? null : digitos(p.telefoneCuidador()));
    }

    private static String digitos(Telefone telefone) {
        return telefone == null ? null : telefone.apenasDigitos();
    }
}
