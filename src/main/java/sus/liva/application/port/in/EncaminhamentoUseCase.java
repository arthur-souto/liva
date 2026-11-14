package sus.liva.application.port.in;

import sus.liva.domain.model.encaminhamento.Encaminhamento;

import java.util.List;
import java.util.UUID;

public interface EncaminhamentoUseCase {
    Encaminhamento receberNoPlantao(Ator ator, UUID encaminhamentoId);
    Encaminhamento autorizarRetorno(Ator ator, UUID encaminhamentoId);
    Encaminhamento concluir(Ator ator, UUID encaminhamentoId, String desfecho);
    Encaminhamento cancelar(Ator ator, UUID encaminhamentoId, String motivo);
    List<EncaminhamentoRecebido> recebidos(Ator ator);
}
