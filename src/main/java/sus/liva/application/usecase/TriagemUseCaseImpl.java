package sus.liva.application.usecase;

import sus.liva.application.exception.ConflitoException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.ItemDaFila;
import sus.liva.application.port.in.NovoEncaminhamento;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.RamoDeAtendimento;
import sus.liva.application.port.in.TriagemUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.EvolucaoClinicaPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.port.out.TriagemRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.Retificacao;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.StatusTriagem;
import sus.liva.domain.model.triagem.Triagem;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TriagemUseCaseImpl implements TriagemUseCase {
    private final TriagemRepositoryPort triagens;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final AgendamentoRepositoryPort agendamentos;
    private final EvolucaoClinicaPort evolucoes;
    private final PacienteRepositoryPort pacientes;
    private final ProfissionalRepositoryPort profissionais;
    private final TransacaoPort transacao;
    private final AcessoAoRamo acesso;
    private final Auditor auditor;
    private final Clock clock;

    public TriagemUseCaseImpl(TriagemRepositoryPort triagens, EncaminhamentoRepositoryPort encaminhamentos,
                              AgendamentoRepositoryPort agendamentos, EvolucaoClinicaPort evolucoes,
                              PacienteRepositoryPort pacientes, ProfissionalRepositoryPort profissionais,
                              TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        this.triagens = triagens;
        this.encaminhamentos = encaminhamentos;
        this.agendamentos = agendamentos;
        this.evolucoes = evolucoes;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.transacao = transacao;
        this.acesso = new AcessoAoRamo(profissionais);
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
    }

    @Override
    public Triagem registrarChegada(Ator ator, UUID pacienteId) {
        Permissoes.exigir(ator, Papel.RECEPCAO);
        if (pacientes.buscarPaciente(pacienteId).isEmpty()) throw new NaoEncontradoException("Paciente não encontrado: " + pacienteId);

        Triagem triagem = Triagem.registrarChegada(UUID.randomUUID(), pacienteId, ator.usuarioId(), agora());
        triagens.salvarTriagem(triagem);
        auditor.registrar(ator, "REGISTRAR_CHEGADA", "TRIAGEM", triagem.getId(), pacienteId);
        return recarregar(triagem.getId());
    }

    @Override
    public List<ItemDaFila> fila(Ator ator) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.CLINICO_GERAL);
        return triagens.triagensAbertas().stream()
                .map(t -> new ItemDaFila(t.getId(), t.getPacienteId(),
                        pacientes.buscarPaciente(t.getPacienteId()).map(Paciente::nomeCompleto).orElse(null),
                        t.getChegadaEm(), t.getStatus(), t.getProfissionalId(), t.getVersao()))
                .toList();
    }

    @Override
    public Triagem iniciar(Ator ator, UUID triagemId) {
        Permissoes.exigir(ator, Papel.CLINICO_GERAL);

        UUID profissional = Permissoes.profissional(ator);
        Triagem triagem = transacao.executar(() -> {
            Triagem t = paraAtualizar(triagemId);
            t.iniciar(profissional, agora());
            triagens.salvarTriagem(t);
            return t;
        });

        auditor.registrar(ator, "INICIAR_TRIAGEM", "TRIAGEM", triagemId, triagem.getPacienteId());
        return recarregar(triagemId);
    }

    @Override
    public Triagem registrarAvaliacao(Ator ator, UUID triagemId, AvaliacaoClinica avaliacao, SinaisVitais sinais,
                                      Prioridade prioridade, String justificativa, long versao) {
        Permissoes.exigir(ator, Papel.CLINICO_GERAL);

        UUID profissional = Permissoes.profissional(ator);
        validarSinais(sinais);

        Triagem triagem = transacao.executar(() -> {
            Triagem t = paraAtualizar(triagemId);
            conferirVersao(t, versao);
            t.registrarAvaliacao(profissional, avaliacao == null ? AvaliacaoClinica.vazia() : avaliacao,
                    sinais == null ? SinaisVitais.naoColetados() : sinais, prioridade, justificativa);
            triagens.salvarTriagem(t);
            return t;
        });

        auditor.registrar(ator, "REGISTRAR_AVALIACAO", "TRIAGEM", triagemId, triagem.getPacienteId());
        return recarregar(triagemId);
    }

    @Override
    public Encaminhamento encaminhar(Ator ator, UUID triagemId, NovoEncaminhamento novo) {
        Permissoes.exigir(ator, Papel.CLINICO_GERAL);

        UUID profissional = Permissoes.profissional(ator);
        if (novo == null || ValidacaoCadastral.vazio(novo.especialidade()) || novo.modalidade() == null)
            throw new IllegalArgumentException("Informe a especialidade e a modalidade do encaminhamento");
        if (profissionais.buscarEspecialidade(novo.especialidade()).isEmpty())
            throw new IllegalArgumentException("Especialidade desconhecida: " + novo.especialidade());

        if (novo.profissionalDestinoId() != null) {
            Profissional destino = profissionais.buscarProfissional(novo.profissionalDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Profissional de destino não encontrado"));
            if (!destino.atende(novo.especialidade()))
                throw new IllegalArgumentException("O profissional de destino não atende a especialidade escolhida");
        }

        Encaminhamento encaminhamento = transacao.executar(() -> {
            Triagem t = paraAtualizar(triagemId);
            if (!t.responsavel(profissional)) throw new IllegalStateException("A triagem está sendo conduzida por outro profissional");

            Encaminhamento e = Encaminhamento.aPartirDe(UUID.randomUUID(), t, novo.especialidade(),
                    novo.profissionalDestinoId(), novo.modalidade(), novo.prioridade(), novo.justificativa(), profissional, agora());
            encaminhamentos.salvarEncaminhamento(e);
            return e;
        });

        auditor.registrar(ator, "ENCAMINHAR_" + novo.modalidade().name(), "ENCAMINHAMENTO", encaminhamento.getId(),
                encaminhamento.getPacienteId());
        return encaminhamentos.buscarEncaminhamento(encaminhamento.getId()).orElseThrow();
    }

    @Override
    public Triagem finalizar(Ator ator, UUID triagemId, long versao) {
        Permissoes.exigir(ator, Papel.CLINICO_GERAL);

        UUID profissional = Permissoes.profissional(ator);
        Triagem triagem = transacao.executar(() -> {
            Triagem t = paraAtualizar(triagemId);
            conferirVersao(t, versao);
            t.finalizar(profissional, agora());
            triagens.salvarTriagem(t);

            for (Encaminhamento e : encaminhamentos.encaminhamentosDaTriagem(triagemId)) {
                StatusEncaminhamento anterior = e.getStatus();
                e.liberarParaAgendamento();
                if (e.getStatus() != anterior) encaminhamentos.salvarEncaminhamento(e);
            }

            return t;
        });

        auditor.registrar(ator, "FINALIZAR_TRIAGEM", "TRIAGEM", triagemId, triagem.getPacienteId());
        return recarregar(triagemId);
    }

    @Override
    public Triagem cancelar(Ator ator, UUID triagemId, String motivo) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.CLINICO_GERAL);

        Triagem triagem = transacao.executar(() -> {
            Triagem t = paraAtualizar(triagemId);
            if (ator.eh(Papel.RECEPCAO) && t.getStatus() != StatusTriagem.AGUARDANDO_ATENDIMENTO)
                throw new IllegalStateException("A recepção só cancela triagens que ainda não começaram");
            if (ator.eh(Papel.CLINICO_GERAL) && t.getStatus() == StatusTriagem.EM_ANDAMENTO && !t.responsavel(ator.profissionalId()))
                throw new IllegalStateException("A triagem está sendo conduzida por outro profissional");

            t.cancelar(motivo, agora());
            triagens.salvarTriagem(t);

            for (Encaminhamento e : encaminhamentos.encaminhamentosDaTriagem(triagemId)) {
                if (e.getStatus() == StatusEncaminhamento.PENDENTE) {
                    e.cancelar("triagem cancelada", agora());
                    encaminhamentos.salvarEncaminhamento(e);
                }
            }

            return t;
        });

        auditor.registrar(ator, "CANCELAR_TRIAGEM", "TRIAGEM", triagemId, triagem.getPacienteId());
        return recarregar(triagemId);
    }

    @Override
    public Retificacao retificar(Ator ator, UUID triagemId, String texto, String justificativa) {
        Permissoes.exigir(ator, Papel.CLINICO_GERAL);

        UUID profissional = Permissoes.profissional(ator);
        Retificacao retificacao = transacao.executar(() -> {
            Triagem t = paraAtualizar(triagemId);
            if (!t.responsavel(profissional)) throw new IllegalStateException("Somente o profissional responsável pode retificar a triagem");

            Retificacao r = t.retificar(profissional, texto, justificativa, agora());
            triagens.salvarRetificacao(r);
            return r;
        });

        auditor.registrar(ator, "RETIFICAR_TRIAGEM", "TRIAGEM", triagemId, triagens.buscarTriagem(triagemId).orElseThrow().getPacienteId());
        return retificacao;
    }

    @Override
    public Triagem encerrar(Ator ator, UUID triagemId) {
        Permissoes.exigir(ator, Papel.CLINICO_GERAL, Papel.ESPECIALISTA);

        Triagem triagem = transacao.executar(() -> {
            Triagem t = paraAtualizar(triagemId);
            List<Encaminhamento> doRamo = encaminhamentos.encaminhamentosDaTriagem(triagemId);
            boolean autorizado = ator.eh(Papel.CLINICO_GERAL)
                    ? t.responsavel(ator.profissionalId())
                    : acesso.especialistaVinculado(ator.profissionalId(), doRamo, consultasDoRamo(doRamo));
            if (!autorizado) throw Permissoes.negado();
            if (doRamo.stream().anyMatch(e -> !e.getStatus().encerrado()))
                throw new ConflitoException("Conclua ou cancele os encaminhamentos desta triagem antes de encerrá-la");

            t.encerrar(agora());
            triagens.salvarTriagem(t);
            return t;
        });

        auditor.registrar(ator, "ENCERRAR_TRIAGEM", "TRIAGEM", triagemId, triagem.getPacienteId());
        return recarregar(triagemId);
    }

    @Override
    public RamoDeAtendimento detalhar(Ator ator, UUID triagemId) {
        Triagem triagem = recarregar(triagemId);
        List<Encaminhamento> doRamo = encaminhamentos.encaminhamentosDaTriagem(triagemId);
        List<Agendamento> consultas = consultasDoRamo(doRamo);

        acesso.exigirLeituraClinica(ator, triagem.getPacienteId(), doRamo, consultas);
        auditor.registrar(ator, "LER_TRIAGEM", "TRIAGEM", triagemId, triagem.getPacienteId());
        return new RamoDeAtendimento(triagem, doRamo, consultas, triagens.retificacoes(triagemId),
                evolucoes.evolucoesDas(consultas.stream().map(Agendamento::getId).toList()));
    }

    private List<Agendamento> consultasDoRamo(List<Encaminhamento> doRamo) {
        return doRamo.stream().flatMap(e -> agendamentos.consultasDoEncaminhamento(e.getId()).stream()).toList();
    }

    private Triagem paraAtualizar(UUID triagemId) {
        return triagens.buscarTriagemParaAtualizar(triagemId)
                .orElseThrow(() -> new NaoEncontradoException("Triagem não encontrada: " + triagemId));
    }

    private Triagem recarregar(UUID triagemId) {
        return triagens.buscarTriagem(triagemId).orElseThrow(() -> new NaoEncontradoException("Triagem não encontrada: " + triagemId));
    }

    private static void conferirVersao(Triagem t, long versao) {
        if (t.getVersao() != null && t.getVersao() != versao)
            throw new ConflitoException("A triagem foi alterada por outra pessoa. Recarregue antes de salvar.");
    }

    private static void validarSinais(SinaisVitais s) {
        if (s == null) return;

        faixa(s.pressaoSistolica(), 40, 300, "Pressão sistólica");
        faixa(s.pressaoDiastolica(), 20, 200, "Pressão diastólica");
        faixa(s.frequenciaCardiaca(), 20, 250, "Frequência cardíaca");
        faixa(s.frequenciaRespiratoria(), 4, 80, "Frequência respiratória");
        faixa(s.saturacao(), 40, 100, "Saturação");

        if (s.temperatura() != null && (s.temperatura().compareTo(BigDecimal.valueOf(30)) < 0 || s.temperatura().compareTo(BigDecimal.valueOf(45)) > 0))
            throw new IllegalArgumentException("Temperatura fora da faixa plausível (30 a 45 °C)");
    }

    private static void faixa(Integer valor, int minimo, int maximo, String campo) {
        if (valor != null && (valor < minimo || valor > maximo))
            throw new IllegalArgumentException(campo + " fora da faixa plausível (" + minimo + " a " + maximo + ")");
    }

    private Instant agora() {
        return Instant.now(clock);
    }
}
