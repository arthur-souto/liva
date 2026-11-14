package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.exception.AcessoNegadoException;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.FiltroDeAtendimentos;
import sus.liva.application.port.in.FiltroDeConsultas;
import sus.liva.application.port.in.HistoricoDoPacienteUseCase;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.PeriodoDasConsultas;
import sus.liva.application.port.in.RemarcacoesDoMes;
import sus.liva.domain.model.agendamento.StatusAgendamento;

@RestController
@RequestMapping("/api/portal")
@Tag(name = "Portal do paciente", description = "O paciente acompanha as próprias triagens, encaminhamentos e consultas")
public class PortalController {
    private final HistoricoDoPacienteUseCase historico;
    private final AgendaDeConsultasUseCase agenda;

    public PortalController(HistoricoDoPacienteUseCase historico, AgendaDeConsultasUseCase agenda) {
        this.historico = historico;
        this.agenda = agenda;
    }

    public record RemarcacoesResponse(int limite, int usadas, int restantes) {
        static RemarcacoesResponse de(RemarcacoesDoMes r) {
            return new RemarcacoesResponse(r.limite(), r.usadas(), r.restantes());
        }
    }

    @GetMapping("/remarcacoes")
    @Operation(summary = "Quantas remarcações o paciente ainda pode fazer neste mês")
    public RemarcacoesResponse remarcacoes(Ator ator) {
        if (!ator.agePeloPaciente()) throw new AcessoNegadoException("O portal é exclusivo do paciente");
        return RemarcacoesResponse.de(agenda.remarcacoesDoMes(ator));
    }

    @GetMapping("/historico")
    @Operation(summary = "Histórico do paciente logado")
    public HistoricoResponse historico(Ator ator) {
        if (!ator.agePeloPaciente()) throw new AcessoNegadoException("O portal é exclusivo do paciente");
        return HistoricoResponse.de(historico.historico(ator, ator.pacienteId()));
    }

    @GetMapping("/atendimentos")
    @Operation(summary = "Atendimentos do paciente logado, paginados",
            description = "Filtros opcionais: especialidade (código), andamento (ABERTO ou ENCERRADO) e ano. Tamanho máximo da página: 50.")
    public AtendimentosResponse atendimentos(Ator ator, @RequestParam(required = false) String especialidade,
                                             @RequestParam(required = false) String andamento, @RequestParam(required = false) Integer ano,
                                             @RequestParam(required = false) Integer pagina, @RequestParam(required = false) Integer tamanho) {
        if (!ator.agePeloPaciente()) throw new AcessoNegadoException("O portal é exclusivo do paciente");
        return AtendimentosResponse.de(historico.atendimentos(ator, ator.pacienteId(),
                new FiltroDeAtendimentos(especialidade, Requisicoes.andamento(andamento), ano), PedidoDePagina.de(pagina, tamanho)));
    }

    @GetMapping("/consultas")
    @Operation(summary = "Consultas do paciente logado, paginadas",
            description = "periodo PROXIMAS (padrão) ou ANTERIORES. Filtros opcionais: especialidade (nome), situacao, ano e busca "
                    + "(especialidade ou profissional). Tamanho máximo da página: 50.")
    public ConsultasDoPacienteResponse consultas(Ator ator, @RequestParam(defaultValue = "PROXIMAS") PeriodoDasConsultas periodo,
                                                 @RequestParam(required = false) String especialidade,
                                                 @RequestParam(required = false) StatusAgendamento situacao,
                                                 @RequestParam(required = false) Integer ano, @RequestParam(required = false) String busca,
                                                 @RequestParam(required = false) Integer pagina,
                                                 @RequestParam(required = false) Integer tamanho) {
        if (!ator.agePeloPaciente()) throw new AcessoNegadoException("O portal é exclusivo do paciente");
        return ConsultasDoPacienteResponse.de(historico.consultas(ator, ator.pacienteId(),
                new FiltroDeConsultas(periodo, especialidade, situacao, ano, busca), PedidoDePagina.de(pagina, tamanho)));
    }
}
