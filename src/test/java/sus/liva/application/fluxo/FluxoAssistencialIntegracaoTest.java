package sus.liva.application.fluxo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import sus.liva.TestcontainersConfig;
import sus.liva.application.exception.AcessoNegadoException;
import sus.liva.application.exception.ConflitoException;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.AtendimentoEspecializadoUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.CadastroDePacienteUseCase;
import sus.liva.application.port.in.DadosCadastrais;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.EncaminhamentoUseCase;
import sus.liva.application.port.in.HistoricoDoPaciente;
import sus.liva.application.port.in.HistoricoDoPacienteUseCase;
import sus.liva.application.port.in.NovoEncaminhamento;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.RamoDoHistorico;
import sus.liva.application.port.in.TriagemUseCase;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.NaturezaConsulta;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;
import sus.liva.domain.model.paciente.CadastroDePaciente;
import sus.liva.domain.model.paciente.Sexo;
import sus.liva.domain.model.profissional.Disponibilidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.profissional.TipoProfissional;
import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.StatusTriagem;
import sus.liva.domain.model.triagem.Triagem;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"confirmasus.whatsapp.provedor=console", "confirmasus.disparo-diario.cron=-",
        "confirmasus.seguranca.api-key=chave-de-teste"})
@Import({TestcontainersConfig.class, FluxoAssistencialIntegracaoTest.RelogioConfig.class})
class FluxoAssistencialIntegracaoTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate SEGUNDA = LocalDate.of(2030, 1, 7);
    private static final LocalDateTime AS_14 = SEGUNDA.atTime(14, 0);
    private static final LocalDateTime AS_14_30 = SEGUNDA.atTime(14, 30);
    private static final LocalDateTime AS_15 = SEGUNDA.atTime(15, 0);

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioConfig {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    static class RelogioDeTeste extends Clock {
        private volatile Instant agora = Instant.now();

        void em(LocalDateTime momento) {
            agora = momento.atZone(FUSO).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return FUSO;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(agora, zone);
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    @Autowired private RelogioDeTeste relogio;
    @Autowired private CadastroDePacienteUseCase cadastro;
    @Autowired private TriagemUseCase triagens;
    @Autowired private EncaminhamentoUseCase encaminhamentos;
    @Autowired private AgendaDeConsultasUseCase agenda;
    @Autowired private AtendimentoEspecializadoUseCase atendimento;
    @Autowired private HistoricoDoPacienteUseCase historico;
    @Autowired private DispararLembreteUseCase dispararLembrete;
    @Autowired private ProcessarRespostaUseCase processarResposta;
    @Autowired private ProfissionalRepositoryPort profissionais;
    @Autowired private EncaminhamentoRepositoryPort encaminhamentoRepository;
    @Autowired private JdbcTemplate jdbc;

    private final Ator recepcao = Ator.equipe(UUID.randomUUID(), Papel.RECEPCAO);
    private Profissional clinico;
    private Ator clinicoAtor;

    @BeforeEach
    void preparar() {
        relogio.em(SEGUNDA.atTime(10, 0));
        clinico = profissional(TipoProfissional.CLINICO_GERAL, null);
        clinicoAtor = Ator.profissional(UUID.randomUUID(), Papel.CLINICO_GERAL, clinico.id());
    }

    @Test
    void cadastraPacienteERecusaDocumentoDuplicado() {
        String cpf = cpfValido();
        CadastroDePaciente criado = cadastro.cadastrar(recepcao, dados("Maria da Silva", cpf));

        assertThat(cadastro.pesquisar(recepcao, cpf)).extracting(c -> c.paciente().id()).containsExactly(criado.paciente().id());
        assertThat(cadastro.pesquisar(recepcao, "maria da sil")).extracting(c -> c.paciente().id()).contains(criado.paciente().id());
        assertThatThrownBy(() -> cadastro.cadastrar(recepcao, dados("Outra Maria", cpf)))
                .isInstanceOf(ConflitoException.class).hasMessageContaining("CPF");
        assertThatThrownBy(() -> cadastro.cadastrar(recepcao, dados("Sem documento", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadastroSimultaneoComMesmoCpfGeraUmUnicoPaciente() throws Exception {
        String cpf = cpfValido();
        List<Resultado<CadastroDePaciente>> resultados = emParalelo(6, i -> () -> cadastro.cadastrar(recepcao, dados("Paciente " + i, cpf)));

        assertThat(resultados).filteredOn(Resultado::sucesso).hasSize(1);
        assertThat(resultados).filteredOn(r -> !r.sucesso()).allSatisfy(r -> assertThat(r.erro()).isInstanceOf(ConflitoException.class));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pacientes WHERE cpf = ?", Integer.class, cpf)).isEqualTo(1);
    }

    @Test
    void atualizacaoComVersaoDesatualizadaEhRecusada() {
        CadastroDePaciente criado = cadastro.cadastrar(recepcao, dados("João Pereira", cpfValido()));
        DadosCadastrais novos = dados("João Pereira Lima", criado.paciente().cpf());

        CadastroDePaciente atualizado = cadastro.atualizar(recepcao, criado.paciente().id(), novos, criado.versao());

        assertThat(atualizado.versao()).isGreaterThan(criado.versao());
        assertThatThrownBy(() -> cadastro.atualizar(recepcao, criado.paciente().id(), novos, criado.versao()))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void portalSoEhLiberadoDepoisDaPrimeiraTriagemFinalizada() {
        UUID paciente = novoPaciente();
        assertThat(historico.podeAcessarPortal(paciente)).isFalse();

        Triagem t = triagemEmAndamento(paciente);
        assertThat(historico.podeAcessarPortal(paciente)).isFalse();

        finalizar(t);
        assertThat(historico.podeAcessarPortal(paciente)).isTrue();
    }

    @Test
    void pacienteNaoEntraDuasVezesNaFila() {
        UUID paciente = novoPaciente();
        triagens.registrarChegada(recepcao, paciente);

        assertThatThrownBy(() -> triagens.registrarChegada(recepcao, paciente)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void fluxoCompletoDoEncaminhamentoAteAlta() {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        Ator especialista = Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, ortopedista.id());
        UUID paciente = novoPaciente();
        Ator portal = Ator.paciente(UUID.randomUUID(), paciente);

        Triagem t = triagemEmAndamento(paciente);
        Encaminhamento e = triagens.encaminhar(clinicoAtor, t.getId(), eletivo("ORTOPEDIA", ortopedista.id()));
        assertThat(e.getStatus()).isEqualTo(StatusEncaminhamento.PENDENTE);
        assertThatThrownBy(() -> agenda.agendar(portal, e.getId(), ortopedista.id(), AS_14, null))
                .isInstanceOf(IllegalStateException.class);

        finalizar(t);
        assertThat(encaminhamento(e.getId()).getStatus()).isEqualTo(StatusEncaminhamento.AGUARDANDO_AGENDAMENTO);
        assertThat(agenda.horariosDisponiveis(portal, e.getId(), SEGUNDA, SEGUNDA))
                .extracting(h -> h.inicio()).contains(AS_14, AS_14_30, AS_15);

        Agendamento inicial = agenda.agendar(portal, e.getId(), ortopedista.id(), AS_14, null);
        assertThat(inicial.getNatureza()).isEqualTo(NaturezaConsulta.INICIAL);
        assertThat(inicial.getTriagemId()).isEqualTo(t.getId());
        assertThat(encaminhamento(e.getId()).getStatus()).isEqualTo(StatusEncaminhamento.AGENDADO);
        assertThatThrownBy(() -> agenda.horariosDisponiveis(portal, e.getId(), SEGUNDA, SEGUNDA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> agenda.agendar(portal, e.getId(), ortopedista.id(), AS_15, null)).isInstanceOf(IllegalStateException.class);
        assertThat(atendimento.agenda(especialista, SEGUNDA, SEGUNDA)).extracting(c -> c.consulta().getId()).contains(inicial.getId());

        Agendamento emCurso = atendimento.iniciar(especialista, inicial.getId(), inicial.getVersao());
        atendimento.registrarEvolucao(especialista, inicial.getId(), "Edema leve, solicitado raio-x");
        Agendamento realizada = atendimento.finalizar(especialista, inicial.getId(), "Retorno com exame", true, emCurso.getVersao());
        assertThat(realizada.getStatus()).isEqualTo(StatusAgendamento.COMPARECEU);
        assertThat(encaminhamento(e.getId()).getStatus()).isEqualTo(StatusEncaminhamento.AGUARDANDO_AGENDAMENTO);

        Agendamento retorno = agenda.agendar(portal, e.getId(), ortopedista.id(), AS_15, null);
        assertThat(retorno.getNatureza()).isEqualTo(NaturezaConsulta.RETORNO);
        assertThatThrownBy(() -> triagens.encerrar(clinicoAtor, t.getId())).isInstanceOf(ConflitoException.class);

        Agendamento retornoEmCurso = atendimento.iniciar(especialista, retorno.getId(), retorno.getVersao());
        atendimento.finalizar(especialista, retorno.getId(), "Exame normal", false, retornoEmCurso.getVersao());
        encaminhamentos.concluir(especialista, e.getId(), "Alta ortopédica");
        Triagem encerrada = triagens.encerrar(especialista, t.getId());

        assertThat(encerrada.getStatus()).isEqualTo(StatusTriagem.ENCERRADA);
        assertThat(triagens.detalhar(especialista, t.getId()).evolucoes()).hasSize(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auditoria WHERE paciente_id = ?", Integer.class, paciente)).isGreaterThan(5);
    }

    @Test
    void casoGraveEhEncaminhadoImediatamenteAoPlantao() {
        Profissional plantonista = profissional(TipoProfissional.ESPECIALISTA, "PRONTO_ATENDIMENTO");
        Ator plantao = Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, plantonista.id());
        UUID paciente = novoPaciente();
        Triagem t = triagemEmAndamento(paciente);

        assertThatThrownBy(() -> triagens.encaminhar(clinicoAtor, t.getId(), new NovoEncaminhamento("PRONTO_ATENDIMENTO", null,
                Modalidade.IMEDIATO, Prioridade.URGENTE, "Dor torácica"))).isInstanceOf(IllegalArgumentException.class);

        Encaminhamento imediato = triagens.encaminhar(clinicoAtor, t.getId(), new NovoEncaminhamento("PRONTO_ATENDIMENTO", null,
                Modalidade.IMEDIATO, Prioridade.EMERGENCIA, "Dor torácica com sudorese"));
        assertThat(encaminhamentos.recebidos(plantao)).extracting(r -> r.encaminhamento().getId()).contains(imediato.getId());

        Encaminhamento recebido = encaminhamentos.receberNoPlantao(plantao, imediato.getId());
        assertThat(recebido.getStatus()).isEqualTo(StatusEncaminhamento.EM_ATENDIMENTO);
        assertThatThrownBy(() -> encaminhamentos.receberNoPlantao(plantao, imediato.getId())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void novaTriagemAbreUmRamoIndependente() {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        Profissional cardiologista = especialistaComAgenda("CARDIOLOGIA");
        Ator ortopedia = Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, ortopedista.id());
        UUID paciente = novoPaciente();
        Ator portal = Ator.paciente(UUID.randomUUID(), paciente);

        Triagem primeira = triagemEmAndamento(paciente);
        Encaminhamento joelho = triagens.encaminhar(clinicoAtor, primeira.getId(), eletivo("ORTOPEDIA", null));
        finalizar(primeira);
        agenda.agendar(portal, joelho.getId(), ortopedista.id(), AS_14, null);

        Triagem segunda = triagemEmAndamento(paciente);
        Encaminhamento coracao = triagens.encaminhar(clinicoAtor, segunda.getId(), eletivo("CARDIOLOGIA", null));
        finalizar(segunda);
        agenda.agendar(portal, coracao.getId(), cardiologista.id(), AS_14, null);

        HistoricoDoPaciente visaoDoPaciente = historico.historico(portal, paciente);
        assertThat(visaoDoPaciente.ramos()).hasSize(2).allSatisfy(r -> {
            assertThat(r.encaminhamentos()).hasSize(1);
            assertThat(r.consultas()).hasSize(1);
        });

        HistoricoDoPaciente visaoDoOrtopedista = historico.historico(ortopedia, paciente);
        assertThat(visaoDoOrtopedista.ramos()).filteredOn(RamoDoHistorico::acessoClinico)
                .extracting(r -> r.triagem().id()).containsExactly(primeira.getId());
        assertThat(visaoDoOrtopedista.ramos()).filteredOn(r -> !r.acessoClinico())
                .allSatisfy(r -> assertThat(r.triagem().queixaPrincipal()).isNull());
        assertThatThrownBy(() -> triagens.detalhar(ortopedia, segunda.getId())).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void acessosIndevidosSaoNegados() {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        UUID dono = novoPaciente();
        UUID outro = novoPaciente();
        Ator intruso = Ator.paciente(UUID.randomUUID(), outro);
        Triagem t = triagemEmAndamento(dono);
        Encaminhamento e = triagens.encaminhar(clinicoAtor, t.getId(), eletivo("ORTOPEDIA", null));
        finalizar(t);

        assertThatThrownBy(() -> agenda.agendar(intruso, e.getId(), ortopedista.id(), AS_14, null)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> historico.historico(intruso, dono)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> cadastro.buscar(intruso, dono)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> triagens.iniciar(recepcao, t.getId())).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> cadastro.cadastrar(clinicoAtor, dados("X", cpfValido()))).isInstanceOf(AcessoNegadoException.class);
        Profissional cardiologista = profissional(TipoProfissional.ESPECIALISTA, "CARDIOLOGIA");
        assertThatThrownBy(() -> triagens.detalhar(Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, cardiologista.id()), t.getId()))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamentos WHERE paciente_id = ?", Integer.class, dono)).isZero();
    }

    @Test
    void agendamentoSimultaneoDoMesmoHorarioReservaApenasUmaVez() throws Exception {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        int concorrentes = 5;
        List<Encaminhamento> liberados = new ArrayList<>();
        for (int i = 0; i < concorrentes; i++) liberados.add(encaminhamentoLiberado(novoPaciente(), "ORTOPEDIA"));

        List<Resultado<Agendamento>> resultados = emParalelo(concorrentes, i -> () -> agenda.agendar(
                Ator.paciente(UUID.randomUUID(), liberados.get(i).getPacienteId()), liberados.get(i).getId(), ortopedista.id(), AS_14, null));

        assertThat(resultados).filteredOn(Resultado::sucesso).hasSize(1);
        assertThat(resultados).filteredOn(r -> !r.sucesso()).allSatisfy(r -> assertThat(r.erro()).isInstanceOf(ConflitoException.class));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamentos WHERE profissional_id = ? AND status <> 'CANCELADO'",
                Integer.class, ortopedista.id())).isEqualTo(1);
        assertThat(liberados).extracting(e -> encaminhamento(e.getId()).getStatus())
                .containsOnlyOnce(StatusEncaminhamento.AGENDADO);
    }

    @Test
    void repeticaoComMesmaChaveNaoDuplicaOAgendamento() throws Exception {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        Encaminhamento e = encaminhamentoLiberado(novoPaciente(), "ORTOPEDIA");
        Ator portal = Ator.paciente(UUID.randomUUID(), e.getPacienteId());
        String chave = "chave-" + UUID.randomUUID();

        List<Resultado<Agendamento>> resultados = emParalelo(4, i -> () -> agenda.agendar(portal, e.getId(), ortopedista.id(), AS_14, chave));

        assertThat(resultados).allSatisfy(r -> assertThat(r.sucesso()).isTrue());
        assertThat(resultados).extracting(r -> r.valor().getId()).containsOnly(resultados.get(0).valor().getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamentos WHERE encaminhamento_id = ?", Integer.class, e.getId())).isEqualTo(1);
    }

    @Test
    void remarcacaoQueFalhaDesfazOCancelamento() {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        Encaminhamento a = encaminhamentoLiberado(novoPaciente(), "ORTOPEDIA");
        Encaminhamento b = encaminhamentoLiberado(novoPaciente(), "ORTOPEDIA");
        Agendamento consultaA = agenda.agendar(recepcao, a.getId(), ortopedista.id(), AS_14, null);
        agenda.agendar(recepcao, b.getId(), ortopedista.id(), AS_14_30, null);

        assertThatThrownBy(() -> agenda.remarcar(recepcao, consultaA.getId(), ortopedista.id(), AS_14_30, consultaA.getVersao()))
                .isInstanceOf(ConflitoException.class);

        assertThat(jdbc.queryForObject("SELECT status FROM agendamentos WHERE id = ?", String.class, consultaA.getId())).isEqualTo("AGENDADO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamentos WHERE encaminhamento_id = ?", Integer.class, a.getId())).isEqualTo(1);
        assertThatThrownBy(() -> agenda.cancelar(recepcao, consultaA.getId(), consultaA.getVersao() + 7)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void triagemComVersaoDesatualizadaNaoSobrescreve() {
        Triagem t = triagemEmAndamento(novoPaciente());
        AvaliacaoClinica avaliacao = new AvaliacaoClinica("Cefaleia", null, null, null, null);
        triagens.registrarAvaliacao(clinicoAtor, t.getId(), avaliacao, SinaisVitais.naoColetados(), Prioridade.URGENTE, null, t.getVersao());

        assertThatThrownBy(() -> triagens.registrarAvaliacao(clinicoAtor, t.getId(), avaliacao, SinaisVitais.naoColetados(),
                Prioridade.NAO_URGENTE, null, t.getVersao())).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> triagens.registrarAvaliacao(clinicoAtor, t.getId(), avaliacao,
                new SinaisVitais(120, 80, 400, 16, new BigDecimal("36.5"), 98), Prioridade.URGENTE, null,
                triagens.detalhar(clinicoAtor, t.getId()).triagem().getVersao())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void respostaPeloWhatsappLiberaOEncaminhamentoParaNovoAgendamento() {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        String telefone = telefoneAleatorio();
        UUID paciente = cadastro.cadastrar(recepcao, dadosComTelefone("Ana Souza", cpfValido(), telefone)).paciente().id();
        Encaminhamento e = encaminhamentoLiberado(paciente, "ORTOPEDIA");
        Agendamento consulta = agenda.agendar(recepcao, e.getId(), ortopedista.id(), AS_14, null);
        dispararLembrete.disparar(consulta.getId(), TipoLembrete.PEDIDO_CONFIRMACAO, true);

        processarResposta.processar(Telefone.de(telefone), "2", null);

        assertThat(jdbc.queryForObject("SELECT status FROM agendamentos WHERE id = ?", String.class, consulta.getId())).isEqualTo("CANCELADO");
        assertThat(encaminhamento(e.getId()).getStatus()).isEqualTo(StatusEncaminhamento.AGUARDANDO_AGENDAMENTO);
        assertThat(agenda.agendar(recepcao, e.getId(), ortopedista.id(), AS_14, null).getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    void faltaSoEhRegistradaDepoisDoHorario() {
        Profissional ortopedista = especialistaComAgenda("ORTOPEDIA");
        Ator especialista = Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, ortopedista.id());
        Encaminhamento e = encaminhamentoLiberado(novoPaciente(), "ORTOPEDIA");
        Agendamento consulta = agenda.agendar(recepcao, e.getId(), ortopedista.id(), AS_14, null);

        assertThatThrownBy(() -> atendimento.registrarFalta(especialista, consulta.getId(), consulta.getVersao()))
                .isInstanceOf(IllegalStateException.class);

        relogio.em(AS_15);
        Agendamento faltou = atendimento.registrarFalta(especialista, consulta.getId(), consulta.getVersao());

        assertThat(faltou.getStatus()).isEqualTo(StatusAgendamento.FALTOU);
        assertThat(encaminhamento(e.getId()).getStatus()).isEqualTo(StatusEncaminhamento.AGUARDANDO_AGENDAMENTO);
        assertThatThrownBy(() -> atendimento.iniciar(especialista, consulta.getId(), faltou.getVersao())).isInstanceOf(IllegalStateException.class);
    }

    private Encaminhamento encaminhamentoLiberado(UUID paciente, String especialidade) {
        Triagem t = triagemEmAndamento(paciente);
        Encaminhamento e = triagens.encaminhar(clinicoAtor, t.getId(), eletivo(especialidade, null));
        finalizar(t);
        return encaminhamento(e.getId());
    }

    private Triagem triagemEmAndamento(UUID paciente) {
        Triagem chegada = triagens.registrarChegada(recepcao, paciente);
        return triagens.iniciar(clinicoAtor, chegada.getId());
    }

    private void finalizar(Triagem t) {
        Triagem avaliada = triagens.registrarAvaliacao(clinicoAtor, t.getId(),
                new AvaliacaoClinica("Dor persistente", "Dor ao caminhar", null, null, null),
                new SinaisVitais(120, 80, 78, 16, new BigDecimal("36.7"), 98), Prioridade.POUCO_URGENTE, null, t.getVersao());
        triagens.finalizar(clinicoAtor, t.getId(), avaliada.getVersao());
    }

    private Encaminhamento encaminhamento(UUID id) {
        return encaminhamentoRepository.buscarEncaminhamento(id).orElseThrow();
    }

    private static NovoEncaminhamento eletivo(String especialidade, UUID destino) {
        return new NovoEncaminhamento(especialidade, destino, Modalidade.ELETIVO, Prioridade.POUCO_URGENTE, "Avaliação especializada");
    }

    private Profissional especialistaComAgenda(String especialidade) {
        Profissional p = profissional(TipoProfissional.ESPECIALISTA, especialidade);
        profissionais.salvarDisponibilidade(new Disponibilidade(UUID.randomUUID(), p.id(), DayOfWeek.MONDAY,
                LocalTime.of(14, 0), LocalTime.of(15, 30), 30));
        return p;
    }

    private Profissional profissional(TipoProfissional tipo, String especialidade) {
        Profissional p = new Profissional(UUID.randomUUID(), "Profissional " + tipo, "CRM-" + UUID.randomUUID().toString().substring(0, 12),
                tipo, especialidade, "Policlínica Central", "Rua das Flores, 100", true);
        profissionais.salvarProfissional(p);
        return p;
    }

    private UUID novoPaciente() {
        return cadastro.cadastrar(recepcao, dados("Paciente de Teste", cpfValido())).paciente().id();
    }

    private static DadosCadastrais dados(String nome, String cpf) {
        return dadosComTelefone(nome, cpf, telefoneAleatorio());
    }

    private static DadosCadastrais dadosComTelefone(String nome, String cpf, String telefone) {
        return new DadosCadastrais(nome, null, cpf, null, LocalDate.of(1980, 5, 20), Sexo.FEMININO, null, telefone,
                null, null, null, null, null, null, true);
    }

    private static String telefoneAleatorio() {
        return "119" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
    }

    private static String cpfValido() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) d[i] = ThreadLocalRandom.current().nextInt(10);
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder cpf = new StringBuilder();
        for (int n : d) cpf.append(n);
        return cpf.chars().distinct().count() == 1 ? cpfValido() : cpf.toString();
    }

    private static int digito(int[] d, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) soma += d[i] * (posicao + 1 - i);
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }

    private record Resultado<T>(T valor, Throwable erro) {
        boolean sucesso() {
            return erro == null;
        }
    }

    private interface Tarefa<T> {
        Callable<T> para(int indice);
    }

    private static <T> List<Resultado<T>> emParalelo(int quantidade, Tarefa<T> tarefa) throws InterruptedException {
        CountDownLatch largada = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(quantidade)) {
            List<Future<T>> futuros = new ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                Callable<T> c = tarefa.para(i);
                futuros.add(executor.submit(() -> {
                    largada.await();
                    return c.call();
                }));
            }
            largada.countDown();
            List<Resultado<T>> resultados = new ArrayList<>();
            for (Future<T> f : futuros) {
                try {
                    resultados.add(new Resultado<>(f.get(), null));
                } catch (ExecutionException e) {
                    resultados.add(new Resultado<>(null, e.getCause()));
                }
            }
            return resultados;
        }
    }
}
