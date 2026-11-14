package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.paciente.Paciente;

import java.util.List;

public record HistoricoDoPaciente(Paciente paciente, List<RamoDoHistorico> ramos, List<Agendamento> consultasAvulsas,
                                  boolean portalLiberado) {}
