package sus.liva.application.port.in;

import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.Retificacao;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.Triagem;

import java.util.List;
import java.util.UUID;

public interface TriagemUseCase {
    Triagem registrarChegada(Ator ator, UUID pacienteId);
    List<ItemDaFila> fila(Ator ator);
    Triagem iniciar(Ator ator, UUID triagemId);
    Triagem registrarAvaliacao(Ator ator, UUID triagemId, AvaliacaoClinica avaliacao, SinaisVitais sinais,
                               Prioridade prioridade, String justificativa, long versao);
    Encaminhamento encaminhar(Ator ator, UUID triagemId, NovoEncaminhamento novo);
    Triagem finalizar(Ator ator, UUID triagemId, long versao);
    Triagem cancelar(Ator ator, UUID triagemId, String motivo);
    Retificacao retificar(Ator ator, UUID triagemId, String texto, String justificativa);
    Triagem encerrar(Ator ator, UUID triagemId);
    RamoDeAtendimento detalhar(Ator ator, UUID triagemId);
}
