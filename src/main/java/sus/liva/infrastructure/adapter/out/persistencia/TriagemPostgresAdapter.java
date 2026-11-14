package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.TriagemRepositoryPort;
import sus.liva.domain.model.triagem.Retificacao;
import sus.liva.domain.model.triagem.StatusTriagem;
import sus.liva.domain.model.triagem.Triagem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TriagemPostgresAdapter implements TriagemRepositoryPort {
    private static final List<StatusTriagem> ABERTAS = List.of(StatusTriagem.AGUARDANDO_ATENDIMENTO, StatusTriagem.EM_ANDAMENTO);
    private static final List<StatusTriagem> CONCLUIDAS = List.of(StatusTriagem.FINALIZADA, StatusTriagem.ENCERRADA);

    private final TriagemJpaRepository triagens;
    private final RetificacaoJpaRepository retificacoes;

    TriagemPostgresAdapter(TriagemJpaRepository triagens, RetificacaoJpaRepository retificacoes) {
        this.triagens = triagens;
        this.retificacoes = retificacoes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Triagem> buscarTriagem(UUID id) {
        return triagens.findById(id).map(TriagemEntity::paraDominio);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Triagem> buscarTriagemParaAtualizar(UUID id) {
        return triagens.buscarComTrava(id).map(TriagemEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Triagem> triagensDoPaciente(UUID pacienteId) {
        return triagens.findByPacienteIdOrderByChegadaEmDesc(pacienteId).stream().map(TriagemEntity::paraDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Triagem> triagensAbertas() {
        return triagens.findByStatusInOrderByChegadaEm(ABERTAS).stream().map(TriagemEntity::paraDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean possuiTriagemConcluida(UUID pacienteId) {
        return triagens.existsByPacienteIdAndStatusIn(pacienteId, CONCLUIDAS);
    }

    @Override
    @Transactional
    public void salvarTriagem(Triagem triagem) {
        TriagemEntity entidade = triagens.findById(triagem.getId())
                .map(e -> {
                    e.copiar(triagem);
                    return e;
                })
                .orElseGet(() -> TriagemEntity.nova(triagem));
        Restricoes.traduzir(() -> triagens.saveAndFlush(entidade));
    }

    @Override
    @Transactional
    public void salvarRetificacao(Retificacao retificacao) {
        retificacoes.save(RetificacaoEntity.de(retificacao));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Retificacao> retificacoes(UUID triagemId) {
        return retificacoes.findByTriagemIdOrderByRegistradaEm(triagemId).stream().map(RetificacaoEntity::paraDominio).toList();
    }
}
