package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.MensagemRecebidaPort;
import sus.liva.domain.model.mensagem.MensagemRecebida;

import java.util.List;
import java.util.UUID;

@Component
public class MensagemRecebidaPostgresAdapter implements MensagemRecebidaPort {
    private final MensagemRecebidaJpaRepository repository;

    MensagemRecebidaPostgresAdapter(MensagemRecebidaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvarRecebida(MensagemRecebida mensagem) {
        repository.save(MensagemRecebidaEntity.de(mensagem));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensagemRecebida> recebidasDoAgendamento(UUID agendamentoId) {
        return repository.findByAgendamentoIdOrderByRecebidaEmAscIdAsc(agendamentoId).stream()
                .map(MensagemRecebidaEntity::paraDominio)
                .toList();
    }
}
