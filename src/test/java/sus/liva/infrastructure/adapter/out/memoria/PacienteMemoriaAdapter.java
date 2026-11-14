package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.CadastroDePaciente;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacienteMemoriaAdapter implements PacienteRepositoryPort {
    private final Map<UUID, Paciente> dados = new ConcurrentHashMap<>();

    @Override
    public Optional<Paciente> buscarPaciente(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public Optional<Paciente> buscarPorCpf(String cpf) {
        return dados.values().stream().filter(p -> cpf.equals(p.cpf())).findFirst();
    }

    @Override
    public List<Paciente> buscarPorTelefone(Telefone telefone) {
        return dados.values().stream().filter(p -> telefone.equals(p.telefone())).toList();
    }

    @Override
    public void salvarPaciente(Paciente paciente) {
        dados.put(paciente.id(), paciente);
    }

    @Override
    public Optional<Paciente> buscarPorCartaoSus(String cartaoSus) {
        return dados.values().stream().filter(p -> cartaoSus.equals(p.cartaoSus())).findFirst();
    }

    @Override
    public Optional<CadastroDePaciente> buscarCadastro(UUID id) {
        return buscarPaciente(id).map(p -> new CadastroDePaciente(p, 0, Instant.EPOCH, null, null));
    }

    @Override
    public List<CadastroDePaciente> pesquisarCadastros(String nome, String documento, int limite) {
        return dados.values().stream()
                .filter(p -> documento != null ? documento.equals(p.cpf()) || documento.equals(p.cartaoSus())
                        : p.nomeCompleto().toLowerCase().startsWith(nome.toLowerCase()))
                .limit(limite)
                .map(p -> new CadastroDePaciente(p, 0, Instant.EPOCH, null, null))
                .toList();
    }

    @Override
    public CadastroDePaciente cadastrar(Paciente paciente, UUID responsavel, Instant em) {
        dados.put(paciente.id(), paciente);
        return new CadastroDePaciente(paciente, 0, em, responsavel, null);
    }

    @Override
    public CadastroDePaciente atualizarCadastro(Paciente paciente, long versaoEsperada, UUID responsavel, Instant em) {
        dados.put(paciente.id(), paciente);
        return new CadastroDePaciente(paciente, versaoEsperada + 1, Instant.EPOCH, null, em);
    }

    public int total() {
        return dados.size();
    }
}
