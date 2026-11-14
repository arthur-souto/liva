package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.RamoDeAtendimento;
import sus.liva.domain.model.agendamento.EvolucaoClinica;
import sus.liva.domain.model.triagem.Retificacao;

import java.util.List;

public record RamoResponse(TriagemResponse triagem, List<EncaminhamentoResponse> encaminhamentos,
                           List<ConsultaResponse> consultas, List<Retificacao> retificacoes, List<EvolucaoClinica> evolucoes) {

    static RamoResponse de(RamoDeAtendimento r) {
        return new RamoResponse(TriagemResponse.de(r.triagem()),
                r.encaminhamentos().stream().map(EncaminhamentoResponse::de).toList(),
                r.consultas().stream().map(ConsultaResponse::de).toList(), r.retificacoes(), r.evolucoes());
    }
}
