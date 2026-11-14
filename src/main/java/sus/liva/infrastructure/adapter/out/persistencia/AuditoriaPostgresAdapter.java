package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.EventoAuditoria;

@Component
public class AuditoriaPostgresAdapter implements AuditoriaPort {
    private final AuditoriaJpaRepository repository;

    AuditoriaPostgresAdapter(AuditoriaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void registrar(EventoAuditoria evento) {
        repository.save(AuditoriaEntity.de(evento));
    }
}
