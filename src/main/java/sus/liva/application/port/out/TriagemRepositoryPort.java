package sus.liva.application.port.out;

import sus.liva.domain.model.triagem.Retificacao;
import sus.liva.domain.model.triagem.Triagem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TriagemRepositoryPort {
    Optional<Triagem> buscarTriagem(UUID id);
    Optional<Triagem> buscarTriagemParaAtualizar(UUID id);
    List<Triagem> triagensDoPaciente(UUID pacienteId);
    List<Triagem> triagensAbertas();
    boolean possuiTriagemConcluida(UUID pacienteId);
    void salvarTriagem(Triagem triagem);
    void salvarRetificacao(Retificacao retificacao);
    List<Retificacao> retificacoes(UUID triagemId);
}
