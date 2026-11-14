package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.ConsultaDaAgenda;

public record ConsultaDaAgendaResponse(ConsultaResponse consulta, String nomePaciente, EncaminhamentoResponse encaminhamento) {

    static ConsultaDaAgendaResponse de(ConsultaDaAgenda c) {
        return new ConsultaDaAgendaResponse(ConsultaResponse.de(c.consulta()), c.nomePaciente(),
                c.encaminhamento() == null ? null : EncaminhamentoResponse.de(c.encaminhamento()));
    }
}
