package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.EvolucaoClinica;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AtendimentoEspecializadoUseCase {
    List<ConsultaDaAgenda> agenda(Ator ator, LocalDate inicio, LocalDate fim);
    Agendamento iniciar(Ator ator, UUID consultaId, long versao);
    Agendamento finalizar(Ator ator, UUID consultaId, String evolucao, boolean autorizarRetorno, long versao);
    EvolucaoClinica registrarEvolucao(Ator ator, UUID consultaId, String texto);
    Agendamento registrarFalta(Ator ator, UUID consultaId, long versao);
}
