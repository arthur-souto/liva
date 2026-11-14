package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.domain.model.encaminhamento.Encaminhamento;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EncaminhamentoMemoriaAdapter implements EncaminhamentoRepositoryPort {
    private final Map<UUID, Encaminhamento> dados = new ConcurrentHashMap<>();

    @Override
    public Optional<Encaminhamento> buscarEncaminhamento(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public Optional<Encaminhamento> buscarEncaminhamentoParaAtualizar(UUID id) {
        return buscarEncaminhamento(id);
    }

    @Override
    public List<Encaminhamento> encaminhamentosDaTriagem(UUID triagemId) {
        return dados.values().stream().filter(e -> triagemId.equals(e.getTriagemId())).toList();
    }

    @Override
    public List<Encaminhamento> encaminhamentosDoPaciente(UUID pacienteId) {
        return dados.values().stream().filter(e -> pacienteId.equals(e.getPacienteId())).toList();
    }

    @Override
    public List<Encaminhamento> encaminhamentosAbertosDaEspecialidade(String especialidade) {
        return dados.values().stream()
                .filter(e -> especialidade.equals(e.getEspecialidade()) && !e.getStatus().encerrado())
                .toList();
    }

    @Override
    public void salvarEncaminhamento(Encaminhamento encaminhamento) {
        dados.put(encaminhamento.getId(), encaminhamento);
    }
}
