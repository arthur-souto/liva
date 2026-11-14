package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;

import java.util.List;

public record RamoDoHistorico(ResumoDaTriagem triagem, List<ResumoDoEncaminhamento> encaminhamentos,
                              List<Agendamento> consultas, boolean acessoClinico) {}
