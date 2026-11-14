package sus.liva.application.port.in;

import sus.liva.domain.model.cuidador.CuidadorVinculado;

import java.util.List;
import java.util.UUID;

public interface PessoaDeConfiancaUseCase {
    CuidadorVinculado vincular(Ator ator, UUID pacienteId, NovaPessoaDeConfianca dados);
    void revogar(Ator ator, UUID pacienteId, UUID cuidadorId);
    List<CuidadorVinculado> doPaciente(Ator ator, UUID pacienteId);
    void solicitarCodigo(String cpfDoCuidador, String cpfDoPaciente);
    Sessao ativar(String cpfDoCuidador, String cpfDoPaciente, String codigo, String senha);
    Sessao entrar(String cpfDoCuidador, String cpfDoPaciente, String senha);
}
