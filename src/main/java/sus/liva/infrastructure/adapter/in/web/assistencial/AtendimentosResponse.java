package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.AtendimentoDoPaciente;
import sus.liva.application.port.in.AtendimentosDoPaciente;
import sus.liva.application.port.in.ResumoDaTriagem;
import sus.liva.application.port.in.ResumoDoEncaminhamento;

import java.util.List;

public record AtendimentosResponse(List<Atendimento> itens, int pagina, int tamanho, long total, int totalPaginas,
                                   List<String> especialidades, List<Integer> anos) {

    public record Atendimento(int numero, ResumoDaTriagem triagem, List<ResumoDoEncaminhamento> encaminhamentos,
                              List<ConsultaResponse> consultas, boolean acessoClinico) {

        static Atendimento de(AtendimentoDoPaciente a) {
            return new Atendimento(a.numero(), a.ramo().triagem(), a.ramo().encaminhamentos(),
                    a.ramo().consultas().stream().map(ConsultaResponse::de).toList(), a.ramo().acessoClinico());
        }
    }

    static AtendimentosResponse de(AtendimentosDoPaciente a) {
        return new AtendimentosResponse(a.pagina().itens().stream().map(Atendimento::de).toList(), a.pagina().numero(),
                a.pagina().tamanho(), a.pagina().total(), a.pagina().totalPaginas(), a.especialidades(), a.anos());
    }
}
