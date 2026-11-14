package sus.liva.application.port.in;

import java.util.List;

public record AtendimentosDoPaciente(Pagina<AtendimentoDoPaciente> pagina, List<String> especialidades, List<Integer> anos) {}
