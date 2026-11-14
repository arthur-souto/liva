package sus.liva.application.usecase;

import sus.liva.application.exception.ConflitoException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.HorarioDisponivel;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.RemarcacoesDoMes;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.port.out.TriagemRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.NaturezaConsulta;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.fila.VagaLiberada;
import sus.liva.domain.model.profissional.Disponibilidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.triagem.StatusTriagem;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AgendaDeConsultasUseCaseImpl implements AgendaDeConsultasUseCase {
    private static final System.Logger LOG = System.getLogger(AgendaDeConsultasUseCaseImpl.class.getName());
    private static final int MAXIMO_DE_DIAS = 31;
    private static final String FORMATO_CHAVE = "[A-Za-z0-9_-]{8,64}";

    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final TriagemRepositoryPort triagens;
    private final AgendamentoRepositoryPort agendamentos;
    private final ProfissionalRepositoryPort profissionais;
    private final DispararLembreteUseCase dispararLembrete;
    private final TransacaoPort transacao;
    private final Auditor auditor;
    private final NotificacaoAoPaciente notificacao;
    private final ModeloMensagens modelos;
    private final LimiteDeRemarcacoes limite;
    private final FilaDeEsperaRepositoryPort fila;
    private final Clock clock;

    public AgendaDeConsultasUseCaseImpl(EncaminhamentoRepositoryPort encaminhamentos, TriagemRepositoryPort triagens,
                                        AgendamentoRepositoryPort agendamentos, ProfissionalRepositoryPort profissionais,
                                        DispararLembreteUseCase dispararLembrete, TransacaoPort transacao,
                                        AuditoriaPort auditoria, NotificacaoAoPaciente notificacao, ModeloMensagens modelos,
                                        LimiteDeRemarcacoes limite, Clock clock) {
        this(encaminhamentos, triagens, agendamentos, profissionais, dispararLembrete, transacao, auditoria, notificacao, modelos, limite,
                clock, null);
    }

    public AgendaDeConsultasUseCaseImpl(EncaminhamentoRepositoryPort encaminhamentos, TriagemRepositoryPort triagens,
                                        AgendamentoRepositoryPort agendamentos, ProfissionalRepositoryPort profissionais,
                                        DispararLembreteUseCase dispararLembrete, TransacaoPort transacao,
                                        AuditoriaPort auditoria, NotificacaoAoPaciente notificacao, ModeloMensagens modelos,
                                        LimiteDeRemarcacoes limite, Clock clock, FilaDeEsperaRepositoryPort fila) {
        this.encaminhamentos = encaminhamentos;
        this.triagens = triagens;
        this.agendamentos = agendamentos;
        this.profissionais = profissionais;
        this.dispararLembrete = dispararLembrete;
        this.transacao = transacao;
        this.auditor = new Auditor(auditoria, clock);
        this.notificacao = notificacao;
        this.modelos = modelos;
        this.limite = limite;
        this.fila = fila;
        this.clock = clock;
    }

    @Override
    public List<HorarioDisponivel> horariosDisponiveis(Ator ator, UUID encaminhamentoId, LocalDate inicio, LocalDate fim) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO);
        Encaminhamento encaminhamento = encaminhamentos.buscarEncaminhamento(encaminhamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Encaminhamento não encontrado: " + encaminhamentoId));
        Permissoes.exigirPacienteProprio(ator, encaminhamento.getPacienteId());
        if (!encaminhamento.permiteAgendamento() && !encaminhamento.permiteRemarcacao())
            throw new IllegalStateException("Este encaminhamento não está liberado para agendamento. Status atual: " + encaminhamento.getStatus());
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDate de = inicio.isBefore(agora.toLocalDate()) ? agora.toLocalDate() : inicio;
        if (fim.isBefore(de)) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial");
        if (ChronoUnit.DAYS.between(de, fim) >= MAXIMO_DE_DIAS)
            throw new IllegalArgumentException("Consulte no máximo " + MAXIMO_DE_DIAS + " dias por vez");

        List<HorarioDisponivel> livres = new ArrayList<>();
        for (Profissional profissional : candidatos(encaminhamento)) {
            List<Agendamento> ocupadas = agendamentos.consultasDoProfissional(profissional.id(),
                            de.atStartOfDay().minusDays(1), fim.plusDays(1).atStartOfDay()).stream()
                    .filter(c -> c.getStatus().ocupaAgenda())
                    .toList();
            List<VagaLiberada> emOferta = fila == null ? List.of()
                    : fila.vagasEmOfertaParaOutros(profissional.id(), de.atStartOfDay(), fim.plusDays(1).atStartOfDay(),
                    encaminhamento.getPacienteId());
            for (Disponibilidade d : profissionais.disponibilidadesDe(profissional.id()))
                for (LocalDate dia = de; !dia.isAfter(fim); dia = dia.plusDays(1))
                    for (LocalDateTime horario : d.horariosEm(dia)) {
                        LocalDateTime termino = horario.plusMinutes(d.duracaoMinutos());
                        if (horario.isAfter(agora) && ocupadas.stream().noneMatch(c -> sobrepoe(c, horario, termino))
                                && emOferta.stream().noneMatch(v -> v.getInicio().isBefore(termino) && v.getFim().isAfter(horario)))
                            livres.add(new HorarioDisponivel(profissional.id(), profissional.nome(), horario, termino));
                    }
        }
        livres.sort(Comparator.comparing(HorarioDisponivel::inicio).thenComparing(HorarioDisponivel::nomeProfissional));
        return livres;
    }

    @Override
    public Agendamento agendar(Ator ator, UUID encaminhamentoId, UUID profissionalId, LocalDateTime inicio, String chaveIdempotencia) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO);
        if (chaveIdempotencia != null && !chaveIdempotencia.matches(FORMATO_CHAVE))
            throw new IllegalArgumentException("Chave de idempotência inválida");
        Optional<Agendamento> repetido = repetido(chaveIdempotencia, encaminhamentoId);
        if (repetido.isPresent()) return repetido.get();
        exigirFuturo(inicio);

        Reserva reserva = transacao.executar(() -> {
            Encaminhamento e = encaminhamentoParaAtualizar(encaminhamentoId);
            Permissoes.exigirPacienteProprio(ator, e.getPacienteId());
            Optional<Agendamento> concorrente = repetido(chaveIdempotencia, encaminhamentoId);
            if (concorrente.isPresent()) return new Reserva(concorrente.get(), false);
            if (triagens.buscarTriagem(e.getTriagemId()).filter(t -> t.getStatus() == StatusTriagem.FINALIZADA).isEmpty())
                throw new IllegalStateException("A triagem deste encaminhamento não permite novos agendamentos");
            Profissional profissional = profissional(profissionalId);
            int duracao = duracaoNaAgenda(profissional, inicio);
            exigirForaDeOferta(profissional, inicio, duracao, e.getPacienteId());
            boolean jaAtendido = agendamentos.consultasDoEncaminhamento(e.getId()).stream()
                    .anyMatch(c -> c.getStatus() == StatusAgendamento.COMPARECEU);
            Agendamento consulta = Agendamento.consulta(UUID.randomUUID(), e, profissional, inicio, duracao,
                    jaAtendido ? NaturezaConsulta.RETORNO : NaturezaConsulta.INICIAL, chaveIdempotencia);
            e.consultaAgendada();
            encaminhamentos.salvarEncaminhamento(e);
            agendamentos.salvarAgendamento(consulta);
            return new Reserva(consulta, true);
        });

        if (reserva.nova()) {
            auditor.registrar(ator, "AGENDAR_CONSULTA", "CONSULTA", reserva.consulta().getId(), reserva.consulta().getPacienteId());
            notificar(reserva.consulta().getId());
        }
        return agendamentos.buscarAgendamento(reserva.consulta().getId()).orElseThrow();
    }

    @Override
    public Agendamento remarcar(Ator ator, UUID consultaId, UUID profissionalId, LocalDateTime inicio, long versao) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO);
        exigirFuturo(inicio);
        Remarcacao remarcacao = transacao.executar(() -> {
            Agendamento atual = consultaParaAtualizar(consultaId, versao);
            Permissoes.exigirPacienteProprio(ator, atual.getPacienteId());
            if (!atual.doEncaminhamento())
                throw new IllegalArgumentException("Somente consultas de encaminhamento são remarcadas por aqui");
            if (ator.agePeloPaciente()) limite.exigirDisponivel(atual.getPacienteId());
            Encaminhamento e = encaminhamentoParaAtualizar(atual.getEncaminhamentoId());
            Profissional profissional = profissional(profissionalId);
            int duracao = duracaoNaAgenda(profissional, inicio);
            exigirForaDeOferta(profissional, inicio, duracao, atual.getPacienteId());
            atual.cancelar();
            agendamentos.salvarAgendamento(atual);
            Agendamento remarcada = Agendamento.consulta(UUID.randomUUID(), e, profissional, inicio, duracao, atual.getNatureza(), null);
            agendamentos.salvarAgendamento(remarcada);
            limite.registrar(atual.getPacienteId(), atual.getId(), remarcada.getId(),
                    ator.agePeloPaciente() ? LimiteDeRemarcacoes.PORTAL : LimiteDeRemarcacoes.RECEPCAO);
            return new Remarcacao(atual, remarcada);
        });
        Agendamento nova = remarcacao.nova();
        auditor.registrar(ator, "REMARCAR_CONSULTA", "CONSULTA", nova.getId(), nova.getPacienteId());
        notificacao.notificar(nova.getId(), nova.getPacienteId(), p -> modelos.consultaRemarcada(p, remarcacao.anterior(), nova));
        return agendamentos.buscarAgendamento(nova.getId()).orElseThrow();
    }

    @Override
    public Agendamento cancelar(Ator ator, UUID consultaId, long versao) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO, Papel.ESPECIALISTA);
        Agendamento cancelada = transacao.executar(() -> {
            Agendamento atual = consultaParaAtualizar(consultaId, versao);
            Permissoes.exigirPacienteProprio(ator, atual.getPacienteId());
            if (ator.eh(Papel.ESPECIALISTA) && !atual.doProfissional(ator.profissionalId())) throw Permissoes.negado();
            atual.cancelar();
            agendamentos.salvarAgendamento(atual);
            if (atual.doEncaminhamento()) {
                Encaminhamento e = encaminhamentoParaAtualizar(atual.getEncaminhamentoId());
                e.consultaDesmarcada();
                encaminhamentos.salvarEncaminhamento(e);
            }
            return atual;
        });
        auditor.registrar(ator, "CANCELAR_CONSULTA", "CONSULTA", consultaId, cancelada.getPacienteId());
        notificacao.notificar(consultaId, cancelada.getPacienteId(), p -> modelos.consultaCancelada(p, cancelada));
        return agendamentos.buscarAgendamento(consultaId).orElseThrow();
    }

    @Override
    public RemarcacoesDoMes remarcacoesDoMes(Ator ator) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR);
        return limite.doMes(ator.pacienteId());
    }

    private List<Profissional> candidatos(Encaminhamento e) {
        if (e.getProfissionalDestinoId() != null)
            return profissionais.buscarProfissional(e.getProfissionalDestinoId()).filter(p -> p.atende(e.getEspecialidade())).stream().toList();
        return profissionais.especialistasDe(e.getEspecialidade());
    }

    private int duracaoNaAgenda(Profissional profissional, LocalDateTime inicio) {
        return profissionais.disponibilidadesDe(profissional.id()).stream()
                .filter(d -> d.comporta(inicio))
                .findFirst()
                .map(Disponibilidade::duracaoMinutos)
                .orElseThrow(() -> new IllegalArgumentException("Este horário não faz parte da agenda do profissional"));
    }

    private void exigirForaDeOferta(Profissional profissional, LocalDateTime inicio, int duracao, UUID pacienteId) {
        if (fila != null && fila.horarioEmOfertaParaOutro(profissional.id(), inicio, inicio.plusMinutes(duracao), pacienteId))
            throw new ConflitoException("Este horário está reservado para outra pessoa da fila de espera. Escolha outro horário.");
    }

    private Optional<Agendamento> repetido(String chave, UUID encaminhamentoId) {
        if (chave == null) return Optional.empty();
        Optional<Agendamento> existente = agendamentos.buscarPorChaveIdempotencia(chave);
        if (existente.isPresent() && !encaminhamentoId.equals(existente.get().getEncaminhamentoId()))
            throw new ConflitoException("Esta chave de idempotência já foi usada em outro agendamento");
        return existente;
    }

    private Agendamento consultaParaAtualizar(UUID consultaId, long versao) {
        Agendamento atual = agendamentos.buscarAgendamentoParaAtualizar(consultaId)
                .orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada: " + consultaId));
        if (atual.getVersao() != null && atual.getVersao() != versao)
            throw new ConflitoException("A consulta foi alterada por outra pessoa. Recarregue antes de continuar.");
        return atual;
    }

    private Encaminhamento encaminhamentoParaAtualizar(UUID id) {
        return encaminhamentos.buscarEncaminhamentoParaAtualizar(id)
                .orElseThrow(() -> new NaoEncontradoException("Encaminhamento não encontrado: " + id));
    }

    private Profissional profissional(UUID id) {
        if (id == null) throw new IllegalArgumentException("Escolha o profissional");
        return profissionais.buscarProfissional(id).orElseThrow(() -> new NaoEncontradoException("Profissional não encontrado: " + id));
    }

    private void exigirFuturo(LocalDateTime inicio) {
        if (inicio == null || !inicio.isAfter(LocalDateTime.now(clock)))
            throw new IllegalArgumentException("Escolha um horário no futuro");
    }

    private void notificar(UUID consultaId) {
        try {
            dispararLembrete.disparar(consultaId, TipoLembrete.CONFIRMACAO_AGENDAMENTO, false);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Consulta " + consultaId + " agendada, mas a confirmação pelo WhatsApp falhou", e);
        }
    }

    private static boolean sobrepoe(Agendamento c, LocalDateTime inicio, LocalDateTime fim) {
        return c.getDataHora().isBefore(fim) && c.getFim().isAfter(inicio);
    }

    private record Reserva(Agendamento consulta, boolean nova) {}

    private record Remarcacao(Agendamento anterior, Agendamento nova) {}
}
