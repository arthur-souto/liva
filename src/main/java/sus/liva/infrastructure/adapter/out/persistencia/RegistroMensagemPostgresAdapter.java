package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.time.Instant;
import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public boolean enviadaPelaLiva(String idExterno) {
        return repository.existsByIdExterno(idExterno);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean enviadaDesde(Telefone destino, String texto, Instant desde) {
        return repository.existsByChaveDestinoAndTextoAndSucessoTrueAndEnviadoEmAfter(destino.chaveComparacao(), texto, desde);
    }

    @Override
    @Transactional
    public boolean marcarEntregue(String idExterno, Instant em) {
        return repository.marcarEntregue(idExterno, em) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroMensagem> enviadasDoAgendamento(UUID agendamentoId) {
        return repository.findByAgendamentoIdOrderByEnviadoEmAscIdAsc(agendamentoId).stream()
                .map(RegistroMensagemEntity::paraDominio)
                .toList();
    }
}
