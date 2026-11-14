package sus.liva.application.port.in;

import sus.liva.domain.model.paciente.CadastroDePaciente;

import java.util.List;
import java.util.UUID;

public interface CadastroDePacienteUseCase {
    CadastroDePaciente cadastrar(Ator ator, DadosCadastrais dados);
    CadastroDePaciente atualizar(Ator ator, UUID pacienteId, DadosCadastrais dados, long versao);
    CadastroDePaciente buscar(Ator ator, UUID pacienteId);
    List<CadastroDePaciente> pesquisar(Ator ator, String termo);
}
