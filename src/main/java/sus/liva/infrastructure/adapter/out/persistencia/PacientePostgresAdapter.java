package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Pagina;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.CadastroDePaciente;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Component
public class PacientePostgresAdapter implements PacienteRepositoryPort {
    private final PacienteJpaRepository repository;

    PacientePostgresAdapter(PacienteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paciente> buscarPaciente(UUID id) {
        return repository.findById(id).map(PacienteEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paciente> buscarPorCpf(String cpf) {
        return repository.findByCpf(cpf).map(PacienteEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paciente> buscarPorCartaoSus(String cartaoSus) {
        return repository.findByCartaoSus(cartaoSus).map(PacienteEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Paciente> buscarPorTelefone(Telefone telefone) {
        return repository.findByTelefone(telefone.e164()).stream().map(PacienteEntity::paraDominio).toList();
    }

    @Override
    @Transactional
    public void salvarPaciente(Paciente paciente) {
        PacienteEntity entidade = repository.findById(paciente.id())
                .orElseGet(() -> PacienteEntity.novo(paciente, null, Instant.now()));
        entidade.copiar(paciente);
        Restricoes.traduzir(() -> repository.saveAndFlush(entidade));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CadastroDePaciente> buscarCadastro(UUID id) {
        return repository.findById(id).map(PacienteEntity::paraCadastro);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<CadastroDePaciente> pesquisarCadastros(String nome, String documento, PedidoDePagina pedido) {
        String n = Objects.requireNonNullElse(nome, "");
        String d = Objects.requireNonNullElse(documento, "");
        return pagina(p -> repository.pesquisarPaginado(n, d, p), pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<CadastroDePaciente> cadastrosRecentes(PedidoDePagina pedido) {
        return pagina(repository::findAllByOrderByCriadoEmDesc, pedido);
    }

    private static Pagina<CadastroDePaciente> pagina(Function<Pageable, Page<PacienteEntity>> consulta, PedidoDePagina pedido) {
        Page<PacienteEntity> encontrada = consulta.apply(PageRequest.of(pedido.numero() - 1, pedido.tamanho()));
        if (encontrada.isEmpty() && encontrada.getTotalElements() > 0)
            encontrada = consulta.apply(PageRequest.of(encontrada.getTotalPages() - 1, pedido.tamanho()));
        return new Pagina<>(encontrada.getContent().stream().map(PacienteEntity::paraCadastro).toList(), encontrada.getNumber() + 1,
                pedido.tamanho(), encontrada.getTotalElements());
    }

    @Override
    @Transactional
    public CadastroDePaciente cadastrar(Paciente paciente, UUID responsavel, Instant em) {
        return Restricoes.traduzir(() -> repository.saveAndFlush(PacienteEntity.novo(paciente, responsavel, em))).paraCadastro();
    }

    @Override
    @Transactional
    public CadastroDePaciente atualizarCadastro(Paciente paciente, long versaoEsperada, UUID responsavel, Instant em) {
        PacienteEntity entidade = repository.findById(paciente.id())
                .orElseThrow(() -> new NaoEncontradoException("Paciente não encontrado: " + paciente.id()));
        Restricoes.conferirVersao(versaoEsperada, entidade.getVersion(), PacienteEntity.class, paciente.id());
        entidade.copiar(paciente);
        entidade.registrarAtualizacao(responsavel, em);
        return Restricoes.traduzir(() -> repository.saveAndFlush(entidade)).paraCadastro();
    }
}
