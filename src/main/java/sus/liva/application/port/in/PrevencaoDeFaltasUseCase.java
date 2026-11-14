package sus.liva.application.port.in;

import sus.liva.domain.model.ausencia.MotivoDeAusencia;
import sus.liva.domain.model.ausencia.ResultadoDaLigacao;

import java.util.UUID;

public interface PrevencaoDeFaltasUseCase {
    PainelDeFaltas painel(Ator ator, int semanas);
    void registrarMotivo(Ator ator, UUID agendamentoId, MotivoDeAusencia motivo);
    void registrarLigacao(Ator ator, UUID agendamentoId, ResultadoDaLigacao resultado);
}
