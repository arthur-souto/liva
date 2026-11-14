package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AgendaDeConsultasUseCase {
    List<HorarioDisponivel> horariosDisponiveis(Ator ator, UUID encaminhamentoId, LocalDate inicio, LocalDate fim);
    boolean possuiAgenda(Ator ator, UUID encaminhamentoId);
    Agendamento agendar(Ator ator, UUID encaminhamentoId, UUID profissionalId, LocalDateTime inicio, String chaveIdempotencia);
    Agendamento remarcar(Ator ator, UUID consultaId, UUID profissionalId, LocalDateTime inicio, long versao);
    Agendamento cancelar(Ator ator, UUID consultaId, long versao);
    RemarcacoesDoMes remarcacoesDoMes(Ator ator);
}
