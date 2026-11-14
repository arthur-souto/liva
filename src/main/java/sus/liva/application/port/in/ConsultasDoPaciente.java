package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.util.List;

public record ConsultasDoPaciente(Pagina<ConsultaDoPaciente> pagina, long proximas, long anteriores, List<String> especialidades,
                                  List<StatusAgendamento> situacoes, List<Integer> anos) {}
