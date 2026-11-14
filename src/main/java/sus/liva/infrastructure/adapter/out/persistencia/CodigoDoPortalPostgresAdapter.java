package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.CodigoDoPortal;
import sus.liva.application.port.out.CodigoDoPortalPort;

import java.util.Optional;
import java.util.UUID;

@Component
public class CodigoDoPortalPostgresAdapter implements CodigoDoPortalPort {
    private final CodigoPortalJpaRepository repository;

    CodigoDoPortalPostgresAdapter(CodigoPortalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<CodigoDoPortal> buscarParaAtualizar(UUID pacienteId) {
        return repository.buscarComTrava(pacienteId).map(CodigoPortalEntity::paraDominio);
    }

    @Override
    @Transactional
    public void salvarCodigo(CodigoDoPortal c) {
        repository.gravar(c.pacienteId(), c.codigoHash(), c.expiraEm(), c.enviadoEm(), c.tentativas());
    }

    @Override
    @Transactional
    public void removerCodigo(UUID pacienteId) {
        repository.remover(pacienteId);
    }
}
