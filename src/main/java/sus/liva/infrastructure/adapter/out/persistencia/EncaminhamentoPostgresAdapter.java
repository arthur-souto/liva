package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EncaminhamentoPostgresAdapter implements EncaminhamentoRepositoryPort {
    private static final List<StatusEncaminhamento> ABERTOS = Arrays.stream(StatusEncaminhamento.values())
            .filter(s -> !s.encerrado())
            .toList();

    private final EncaminhamentoJpaRepository repository;

    EncaminhamentoPostgresAdapter(EncaminhamentoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Encaminhamento> buscarEncaminhamento(UUID id) {
        return repository.findById(id).map(EncaminhamentoEntity::paraDominio);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Encaminhamento> buscarEncaminhamentoParaAtualizar(UUID id) {
        return repository.buscarComTrava(id).map(EncaminhamentoEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Encaminhamento> encaminhamentosDaTriagem(UUID triagemId) {
        return repository.findByTriagemIdOrderByCriadoEm(triagemId).stream().map(EncaminhamentoEntity::paraDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Encaminhamento> encaminhamentosDoPaciente(UUID pacienteId) {
        return repository.findByPacienteIdOrderByCriadoEm(pacienteId).stream().map(EncaminhamentoEntity::paraDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Encaminhamento> encaminhamentosAbertosDaEspecialidade(String especialidade) {
        return repository.findByEspecialidadeAndStatusInOrderByCriadoEm(especialidade, ABERTOS).stream()
                .map(EncaminhamentoEntity::paraDominio).toList();
    }

    @Override
    @Transactional
    public void salvarEncaminhamento(Encaminhamento encaminhamento) {
        EncaminhamentoEntity entidade = repository.findById(encaminhamento.getId())
                .map(e -> {
                    e.copiar(encaminhamento);
                    return e;
                })
                .orElseGet(() -> EncaminhamentoEntity.novo(encaminhamento));
        Restricoes.traduzir(() -> repository.saveAndFlush(entidade));
    }
}
