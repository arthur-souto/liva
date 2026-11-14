package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.util.Optional;
import java.util.UUID;

@Component
public class RegistroMensagemPostgresAdapter implements RegistroMensagemPort {
    private final RegistroMensagemJpaRepository repository;

    RegistroMensagemPostgresAdapter(RegistroMensagemJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvarRegistro(RegistroMensagem registro) {
        repository.save(RegistroMensagemEntity.de(registro));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean jaEnviado(UUID agendamentoId, TipoLembrete tipo) {
        return repository.existsByAgendamentoIdAndTipoAndSucessoTrue(agendamentoId, tipo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegistroMensagem> ultimoEnvio(Telefone destino, TipoLembrete tipo) {
        return repository.findFirstByChaveDestinoAndTipoAndSucessoTrueOrderByEnviadoEmDescIdDesc(destino.chaveComparacao(), tipo)
                .map(RegistroMensagemEntity::paraDominio);
    }
}
