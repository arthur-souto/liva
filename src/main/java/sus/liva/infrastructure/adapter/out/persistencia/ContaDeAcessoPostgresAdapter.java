package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.out.ContaDeAcesso;
import sus.liva.application.port.out.ContaDeAcessoPort;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class ContaDeAcessoPostgresAdapter implements ContaDeAcessoPort {
    private final UsuarioJpaRepository repository;

    ContaDeAcessoPostgresAdapter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContaDeAcesso> buscarPorLogin(String login) {
        return repository.buscarPorLogin(login).map(UsuarioEntity::paraConta);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContaDeAcesso> buscarPorPaciente(UUID pacienteId) {
        return repository.findByPacienteId(pacienteId).map(UsuarioEntity::paraConta);
    }

    @Override
    @Transactional
    public void criarConta(ContaDeAcesso conta, Instant em) {
        Restricoes.traduzir(() -> repository.saveAndFlush(UsuarioEntity.nova(conta, em)));
    }

    @Override
    @Transactional
    public void redefinirSenha(UUID contaId, String senhaHash) {
        UsuarioEntity usuario = repository.findById(contaId)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado: " + contaId));
        usuario.redefinirSenha(senhaHash);
        repository.saveAndFlush(usuario);
    }

    @Override
    @Transactional
    public void registrarFalha(UUID contaId, int limite, Instant bloquearAte) {
        repository.registrarFalha(contaId, limite, bloquearAte);
    }

    @Override
    @Transactional
    public void registrarAcesso(UUID contaId) {
        repository.registrarAcesso(contaId);
    }
}
