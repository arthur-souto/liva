package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.HistoricoDoPaciente;
import sus.liva.application.port.in.RamoDoHistorico;
import sus.liva.application.port.in.ResumoDaTriagem;
import sus.liva.application.port.in.ResumoDoEncaminhamento;

import java.util.List;

public record HistoricoResponse(PacienteResumoResponse paciente, List<Ramo> ramos, List<ConsultaResponse> consultasAvulsas,
                                boolean portalLiberado) {

    public record Ramo(ResumoDaTriagem triagem, List<ResumoDoEncaminhamento> encaminhamentos, List<ConsultaResponse> consultas,
                       boolean acessoClinico) {

        static Ramo de(RamoDoHistorico r) {
            return new Ramo(r.triagem(), r.encaminhamentos(), r.consultas().stream().map(ConsultaResponse::de).toList(),
                    r.acessoClinico());
        }
    }

    static HistoricoResponse de(HistoricoDoPaciente h) {
        return new HistoricoResponse(PacienteResumoResponse.de(h.paciente()), h.ramos().stream().map(Ramo::de).toList(),
                h.consultasAvulsas().stream().map(ConsultaResponse::de).toList(), h.portalLiberado());
    }
}
