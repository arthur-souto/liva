package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.NaturezaConsulta;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaResponse(UUID id, UUID pacienteId, UUID triagemId, UUID encaminhamentoId, UUID profissionalId,
                               LocalDateTime inicio, LocalDateTime fim, int duracaoMinutos, NaturezaConsulta natureza,
                               StatusAgendamento status, TipoAtendimento tipo, String unidade, String endereco, Long versao) {

    static ConsultaResponse de(Agendamento a) {
        return new ConsultaResponse(a.getId(), a.getPacienteId(), a.getTriagemId(), a.getEncaminhamentoId(),
                a.getProfissionalId(), a.getDataHora(), a.getFim(), a.getDuracaoMinutos(), a.getNatureza(), a.getStatus(),
                a.getTipo(), a.getUnidade(), a.getEndereco(), a.getVersao());
    }
}
