package sus.liva.application.port.out;

import sus.liva.domain.model.encaminhamento.Encaminhamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EncaminhamentoRepositoryPort {
    Optional<Encaminhamento> buscarEncaminhamento(UUID id);
    Optional<Encaminhamento> buscarEncaminhamentoParaAtualizar(UUID id);
    List<Encaminhamento> encaminhamentosDaTriagem(UUID triagemId);
    List<Encaminhamento> encaminhamentosDoPaciente(UUID pacienteId);
    List<Encaminhamento> encaminhamentosAbertosDaEspecialidade(String especialidade);
    void salvarEncaminhamento(Encaminhamento encaminhamento);
}
