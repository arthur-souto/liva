package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.ConsultaDoPaciente;
import sus.liva.application.port.in.ConsultasDoPaciente;
import sus.liva.application.port.in.ResumoDoEncaminhamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.util.List;

public record ConsultasDoPacienteResponse(List<Item> itens, int pagina, int tamanho, long total, int totalPaginas, long proximas,
                                          long anteriores, List<String> especialidades, List<StatusAgendamento> situacoes,
                                          List<Integer> anos) {

    public record Item(ConsultaResponse consulta, ResumoDoEncaminhamento encaminhamento, String especialidade, String profissional) {
        static Item de(ConsultaDoPaciente c) {
            return new Item(ConsultaResponse.de(c.consulta()), c.encaminhamento(), c.especialidade(),
                    c.profissional().isEmpty() ? null : c.profissional());
        }
    }

    static ConsultasDoPacienteResponse de(ConsultasDoPaciente c) {
        return new ConsultasDoPacienteResponse(c.pagina().itens().stream().map(Item::de).toList(), c.pagina().numero(),
                c.pagina().tamanho(), c.pagina().total(), c.pagina().totalPaginas(), c.proximas(), c.anteriores(), c.especialidades(),
                c.situacoes(), c.anos());
    }
}
