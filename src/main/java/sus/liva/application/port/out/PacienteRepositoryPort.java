package sus.liva.application.port.out;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.CadastroDePaciente;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PacienteRepositoryPort {
    Optional<Paciente> buscarPaciente(UUID id);
    Optional<Paciente> buscarPorCpf(String cpf);
    List<Paciente> buscarPorTelefone(Telefone telefone);
    void salvarPaciente(Paciente paciente);
    Optional<Paciente> buscarPorCartaoSus(String cartaoSus);
    Optional<CadastroDePaciente> buscarCadastro(UUID id);
    List<CadastroDePaciente> pesquisarCadastros(String nome, String documento, int limite);
    CadastroDePaciente cadastrar(Paciente paciente, UUID responsavel, Instant em);
    CadastroDePaciente atualizarCadastro(Paciente paciente, long versaoEsperada, UUID responsavel, Instant em);
}
