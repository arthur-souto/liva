package sus.liva.application.port.out;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.cuidador.Cuidador;
import sus.liva.domain.model.cuidador.CuidadorVinculado;
import sus.liva.domain.model.cuidador.VinculoDeCuidado;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuidadorRepositoryPort {
    Optional<Cuidador> buscarCuidador(UUID id);
    Optional<Cuidador> buscarCuidadorPorCpf(String cpf);
    List<Cuidador> buscarCuidadoresPorTelefone(Telefone telefone);
    void salvarCuidador(Cuidador cuidador, Instant em);
    void vincular(VinculoDeCuidado vinculo);
    void revogarVinculo(UUID cuidadorId, UUID pacienteId, Instant em);
    List<CuidadorVinculado> cuidadoresDoPaciente(UUID pacienteId);
    List<UUID> dependentesDe(UUID cuidadorId);
    boolean cuidaDe(UUID cuidadorId, UUID pacienteId);
    Optional<CredencialDoCuidador> credencial(UUID cuidadorId);
    void definirSenha(UUID cuidadorId, String senhaHash);
    void registrarFalha(UUID cuidadorId, int limite, Instant bloquearAte);
    void registrarAcesso(UUID cuidadorId);
    Optional<CodigoDoCuidador> codigoParaAtualizar(UUID cuidadorId);
    void salvarCodigo(UUID cuidadorId, CodigoDoCuidador codigo);
    void removerCodigo(UUID cuidadorId);
}
