package sus.liva.application.fluxo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import sus.liva.TestcontainersConfig;
import sus.liva.application.exception.AcessoNegadoException;
import sus.liva.application.exception.ConflitoException;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.CadastroDePacienteUseCase;
import sus.liva.application.port.in.ConfirmacaoDeEntregaUseCase;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.FilaDeEsperaUseCase;
import sus.liva.application.port.in.FiltroDaFila;
import sus.liva.application.port.in.MinhaFila;
import sus.liva.application.port.in.NovoEncaminhamento;
import sus.liva.application.port.in.OfertaParaPaciente;
import sus.liva.application.port.in.PainelDaFila;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.PosicaoNaFila;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.in.TriagemUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.usecase.LinksDeAcesso;
import sus.liva.application.usecase.ModeloMensagens;
import sus.liva.application.usecase.NotificacaoAoPaciente;
import sus.liva.application.usecase.ProcessarFilaUseCaseImpl;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;
import sus.liva.domain.model.fila.AcaoSemResposta;
import sus.liva.domain.model.fila.PoliticaDaFila;
import sus.liva.domain.model.fila.StatusEntrada;
import sus.liva.domain.model.profissional.Disponibilidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.profissional.TipoProfissional;
import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.Triagem;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.Resultado;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.cpfValido;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.dadosComTelefone;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.emParalelo;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.telefoneAleatorio;

@SpringBootTest(properties = {"confirmasus.whatsapp.provedor=console", "confirmasus.disparo-diario.cron=-",
        "confirmasus.seguranca.api-key=chave-de-teste",
        "confirmasus.seguranca.jwt-segredo=segredo-de-teste-com-pelo-menos-32-caracteres"})
@Import({TestcontainersConfig.class, FluxoAssistencialIntegracaoTest.RelogioConfig.class})
class FilaDeEsperaIntegracaoTest {
    private static final LocalDate SEGUNDA = LocalDate.of(2030, 1, 7);
    private static final LocalDateTime AS_14 = SEGUNDA.atTime(14, 0);
    private static final LocalDateTime AS_14_30 = SEGUNDA.atTime(14, 30);
    private static final LocalDateTime AS_15 = SEGUNDA.atTime(15, 0);

    @Autowired private FluxoAssistencialIntegracaoTest.RelogioDeTeste relogio;
    @Autowired private CadastroDePacienteUseCase cadastro;
    @Autowired private TriagemUseCase triagens;
    @Autowired private AgendaDeConsultasUseCase agenda;
    @Autowired private FilaDeEsperaUseCase fila;
    @Autowired private ProcessarFilaUseCaseImpl processamento;
    @Autowired private ProcessarRespostaUseCase processarResposta;
    @Autowired private DispararLembreteUseCase dispararLembrete;
    @Autowired private ConfirmacaoDeEntregaUseCase entregas;
    @Autowired private ProfissionalRepositoryPort profissionais;
    @Autowired private EncaminhamentoRepositoryPort encaminhamentos;
    @Autowired private AgendamentoRepositoryPort agendamentos;
    @Autowired private FilaDeEsperaRepositoryPort filaRepository;
    @Autowired private NotificacaoAoPaciente notificacao;
    @Autowired private LinksDeAcesso links;
    @Autowired private ModeloMensagens modelos;
    @Autowired private TransacaoPort transacao;
    @Autowired private PoliticaDaFila politica;
    @Autowired private JdbcTemplate jdbc;

    private final Ator recepcao = Ator.equipe(UUID.randomUUID(), Papel.RECEPCAO);
    private Ator clinico;
    private String especialidade;
    private Profissional especialista;
    private LocalDateTime instante;

    @BeforeEach
    void preparar() {
        instante = SEGUNDA.atTime(9, 0);
        relogio.em(instante);
        Profissional clinicoGeral = profissional(TipoProfissional.CLINICO_GERAL, null);
        clinico = Ator.profissional(UUID.randomUUID(), Papel.CLINICO_GERAL, clinicoGeral.id());
        especialidade = novaEspecialidade();
        especialista = especialistaComAgenda(especialidade);
    }

    @Test
    void cadaRamoEntraNaSuaFilaEAPrioridadeClinicaVemAntesDaOrdemDeChegada() {
        Paciente ana = paciente();
        Encaminhamento anaOrtopedia = encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE);
        String outra = novaEspecialidade();
        Encaminhamento anaOutra = encaminhamento(ana.id(), outra, Prioridade.POUCO_URGENTE);
        Paciente bruno = paciente();

        encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);

        processamento.sincronizar(null);
        processamento.sincronizar(null);

        MinhaFila daAna = fila.minhaFila(ana.ator());

        assertThat(daAna.filas()).hasSize(2);

        PosicaoNaFila naPrimeira = filaDe(daAna, anaOrtopedia);
        PosicaoNaFila naSegunda = filaDe(daAna, anaOutra);

        assertThat(naPrimeira.triagemId()).isEqualTo(anaOrtopedia.getTriagemId()).isNotEqualTo(naSegunda.triagemId());
        assertThat(naPrimeira.triagemNumero()).isEqualTo(1);
        assertThat(naSegunda.triagemNumero()).isEqualTo(2);
        assertThat(naPrimeira.posicao()).isEqualTo(2);
        assertThat(naPrimeira.total()).isEqualTo(2);
        assertThat(naPrimeira.aFrenteMaisUrgentes()).isEqualTo(1);
        assertThat(naSegunda.aFrenteMaisUrgentes()).isZero();
        assertThat(naPrimeira.vagasPorSemana()).isZero();
        assertThat(naSegunda.posicao()).isEqualTo(1);
        assertThat(fila.minhaFila(bruno.ator()).filas()).singleElement().extracting(PosicaoNaFila::posicao).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entradas_fila WHERE paciente_id = ?", Integer.class, ana.id())).isEqualTo(2);
    }

    @Test
    void vagaCanceladaEhOferecidaAoProximoElegivelEOAceiteAntecipaAConsultaDoMesmoRamo() {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();
        Encaminhamento doBruno = encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);
        Agendamento consultaDoBruno = agenda.agendar(recepcao, doBruno.getId(), especialista.id(), AS_15, null);
        Paciente carla = paciente();
        Encaminhamento daCarla = encaminhamento(carla.id(), especialidade, Prioridade.NAO_URGENTE);

        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());
        processamento.executarCiclo();

        OfertaParaPaciente oferta = ofertaUnica(bruno);

        assertThat(oferta.inicio()).isEqualTo(AS_14);
        assertThat(oferta.antecipaDe()).isEqualTo(AS_15);
        assertThat(fila.minhaFila(carla.ator()).ofertas()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM ofertas_de_vaga WHERE id = ?", String.class, oferta.id())).isEqualTo("ENVIADA");
        assertThat(agenda.horariosDisponiveis(carla.ator(), daCarla.getId(), SEGUNDA, SEGUNDA))
                .noneMatch(h -> h.inicio().equals(AS_14));
        assertThatThrownBy(() -> agenda.agendar(carla.ator(), daCarla.getId(), especialista.id(), AS_14, null))
                .isInstanceOf(ConflitoException.class);

        Agendamento antecipada = fila.aceitar(bruno.ator(), oferta.id());

        assertThat(antecipada.getDataHora()).isEqualTo(AS_14);
        assertThat(antecipada.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(antecipada.getEncaminhamentoId()).isEqualTo(doBruno.getId());
        assertThat(antecipada.getTriagemId()).isEqualTo(doBruno.getTriagemId());
        assertThat(status(consultaDoBruno.getId())).isEqualTo("CANCELADO");
        assertThat(encaminhamentos.buscarEncaminhamento(doBruno.getId()).orElseThrow().getStatus()).isEqualTo(StatusEncaminhamento.AGENDADO);
        assertThat(jdbc.queryForObject("SELECT status FROM entradas_fila WHERE encaminhamento_id = ?", String.class, doBruno.getId()))
                .isEqualTo("ATENDIDA");
        assertThat(jdbc.queryForObject("SELECT status FROM vagas_liberadas WHERE agendamento_origem_id = ?", String.class,
                consultaDaAna.getId())).isEqualTo("PREENCHIDA");

        processamento.executarCiclo();

        assertThat(fila.minhaFila(ana.ator()).ofertas()).singleElement().extracting(OfertaParaPaciente::inicio).isEqualTo(AS_15);
        assertThat(fila.minhaFila(carla.ator()).ofertas()).isEmpty();

        fila.recusar(ana.ator(), ofertaUnica(ana).id());
        OfertaParaPaciente emCascata = ofertaUnica(carla);

        assertThat(emCascata.inicio()).isEqualTo(AS_15);
        assertThat(jdbc.queryForObject("SELECT origem FROM vagas_liberadas WHERE agendamento_origem_id = ?", String.class,
                consultaDoBruno.getId())).isEqualTo("ANTECIPACAO");
    }

    @Test
    void recusaEExpiracaoNaoTiramNinguemDaFilaNemPermitemAceiteAtrasado() {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();
        Encaminhamento doBruno = encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);
        Paciente carla = paciente();

        encaminhamento(carla.id(), especialidade, Prioridade.POUCO_URGENTE);
        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());
        processamento.executarCiclo();
        int posicaoDoBruno = fila.minhaFila(bruno.ator()).filas().getFirst().posicao();

        fila.recusar(bruno.ator(), ofertaUnica(bruno).id());

        assertThat(fila.minhaFila(bruno.ator()).ofertas()).isEmpty();

        PosicaoNaFila brunoDepois = filaDe(fila.minhaFila(bruno.ator()), doBruno);

        assertThat(brunoDepois.status()).isEqualTo(StatusEntrada.AGUARDANDO);
        assertThat(brunoDepois.posicao()).isEqualTo(posicaoDoBruno);

        OfertaParaPaciente daCarla = ofertaUnica(carla);

        assertThat(daCarla.inicio()).isEqualTo(AS_14);

        relogio.em(AS_14.minus(politica.antecedenciaMinima()).plusSeconds(1));
        processamento.executarCiclo();

        assertThat(jdbc.queryForObject("SELECT status FROM ofertas_de_vaga WHERE id = ?", String.class, daCarla.id())).isEqualTo("EXPIRADA");
        assertThat(jdbc.queryForObject("SELECT status FROM vagas_liberadas WHERE agendamento_origem_id = ?", String.class,
                consultaDaAna.getId())).isEqualTo("ENCERRADA");
        assertThatThrownBy(() -> fila.aceitar(carla.ator(), daCarla.id())).isInstanceOf(ConflitoException.class);
        assertThat(fila.minhaFila(carla.ator()).filas()).singleElement().extracting(PosicaoNaFila::status).isEqualTo(StatusEntrada.AGUARDANDO);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamentos WHERE profissional_id = ? AND data_hora = ? AND status <> 'CANCELADO'",
                Integer.class, especialista.id(), AS_14)).isZero();
    }

    @Test
    void aceitesSimultaneosEAgendamentoConcorrenteReservamOHorarioUmaUnicaVez() throws Exception {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();

        encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);

        Paciente carla = paciente();
        Encaminhamento daCarla = encaminhamento(carla.id(), especialidade, Prioridade.NAO_URGENTE);

        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());
        processamento.executarCiclo();

        OfertaParaPaciente oferta = ofertaUnica(bruno);
        UUID vaga = jdbc.queryForObject("SELECT vaga_id FROM ofertas_de_vaga WHERE id = ?", UUID.class, oferta.id());

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO ofertas_de_vaga (id, vaga_id, entrada_id, paciente_id, status, criada_em, expira_em)
                SELECT gen_random_uuid(), ?, e.id, e.paciente_id, 'PENDENTE', now(), now() + interval '1 hour'
                FROM entradas_fila e WHERE e.encaminhamento_id = ?""", vaga, daCarla.getId()))
                .isInstanceOf(DuplicateKeyException.class);

        List<Resultado<Agendamento>> resultados = emParalelo(6, i -> () -> i == 0
                ? agenda.agendar(carla.ator(), daCarla.getId(), especialista.id(), AS_14, null)
                : fila.aceitar(bruno.ator(), oferta.id()));

        assertThat(resultados.subList(1, 6)).allSatisfy(r -> assertThat(r.sucesso()).isTrue());
        assertThat(resultados.subList(1, 6)).extracting(r -> r.valor().getId()).containsOnly(resultados.get(1).valor().getId());
        assertThat(resultados.getFirst().erro()).isInstanceOf(ConflitoException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamentos WHERE profissional_id = ? AND data_hora = ? AND status <> 'CANCELADO'",
                Integer.class, especialista.id(), AS_14)).isEqualTo(1);
    }

    @Test
    void ciclosConcorrentesOferecemCadaVagaAUmaUnicaPessoa() throws Exception {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);

        for (int i = 0; i < 3; i++) encaminhamento(paciente().id(), especialidade, Prioridade.POUCO_URGENTE);
        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());

        List<Resultado<Object>> ciclos = emParalelo(4, i -> () -> processamento.executarCiclo());

        assertThat(ciclos).allSatisfy(r -> assertThat(r.sucesso()).isTrue());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vagas_liberadas WHERE agendamento_origem_id = ?", Integer.class,
                consultaDaAna.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM ofertas_de_vaga o JOIN vagas_liberadas v ON v.id = o.vaga_id
                WHERE v.agendamento_origem_id = ? AND o.status IN ('PENDENTE', 'ENVIADA')""", Integer.class, consultaDaAna.getId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entradas_fila WHERE especialidade_codigo = ?", Integer.class, especialidade))
                .isEqualTo(4);
    }

    @Test
    void horarioOcupadoNaAgendaDuranteAOfertaCancelaOAceiteSemMexerNaConsultaAtual() {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();
        Encaminhamento doBruno = encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);
        Agendamento consultaDoBruno = agenda.agendar(recepcao, doBruno.getId(), especialista.id(), AS_15, null);

        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());
        processamento.executarCiclo();
        OfertaParaPaciente oferta = ofertaUnica(bruno);
        jdbc.update("""
                INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, status, profissional_id, duracao_minutos, natureza)
                VALUES (?, ?, ?, 'CONSULTA', 'Policlínica Central', 'Rua das Flores, 100', 'AGENDADO', ?, 30, 'AVULSA')""",
                UUID.randomUUID(), ana.id(), AS_14, especialista.id());

        assertThatThrownBy(() -> fila.aceitar(bruno.ator(), oferta.id())).isInstanceOf(ConflitoException.class);

        assertThat(jdbc.queryForObject("SELECT status FROM ofertas_de_vaga WHERE id = ?", String.class, oferta.id())).isEqualTo("CANCELADA");
        assertThat(jdbc.queryForObject("SELECT status FROM vagas_liberadas WHERE agendamento_origem_id = ?", String.class,
                consultaDaAna.getId())).isEqualTo("INDISPONIVEL");
        assertThat(status(consultaDoBruno.getId())).isEqualTo("AGENDADO");
        assertThat(filaDe(fila.minhaFila(bruno.ator()), doBruno).status()).isEqualTo(StatusEntrada.AGUARDANDO);
    }

    @Test
    void respostaDuplicadaPeloWhatsappReservaUmaUnicaConsulta() throws Exception {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();
        Encaminhamento doBruno = encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);

        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());
        processamento.executarCiclo();

        List<Resultado<ResultadoResposta>> respostas = emParalelo(3, i ->
                () -> processarResposta.processar(Telefone.de(bruno.telefone()), "Aceito", "mensagem-repetida-" + bruno.id()));

        assertThat(respostas).allSatisfy(r -> assertThat(r.sucesso()).isTrue());
        assertThat(respostas).extracting(r -> r.valor().agendamentoId()).containsOnly(respostas.getFirst().valor().agendamentoId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamentos WHERE encaminhamento_id = ? AND status = 'CONFIRMADO'",
                Integer.class, doBruno.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT canal_resposta FROM ofertas_de_vaga WHERE paciente_id = ?", String.class, bruno.id()))
                .isEqualTo("WHATSAPP");
    }

    @Test
    void prazoSemRespostaSegueAPoliticaENaoTrataFalhaDeEntregaComoRecusa() {
        Paciente semEntrega = paciente();
        Agendamento consultaSemEntrega = agenda.agendar(recepcao,
                encaminhamento(semEntrega.id(), especialidade, Prioridade.NAO_URGENTE).getId(), especialista.id(), AS_14, null);
        Paciente entregue = paciente();
        Encaminhamento doEntregue = encaminhamento(entregue.id(), especialidade, Prioridade.NAO_URGENTE);
        Agendamento consultaEntregue = agenda.agendar(recepcao, doEntregue.getId(), especialista.id(), AS_14_30, null);

        dispararLembrete.disparar(consultaSemEntrega.getId(), TipoLembrete.PEDIDO_CONFIRMACAO, true);
        dispararLembrete.disparar(consultaEntregue.getId(), TipoLembrete.PEDIDO_CONFIRMACAO, true);
        jdbc.queryForList("SELECT id_externo FROM registros_mensagem WHERE agendamento_id = ? AND tipo = 'PEDIDO_CONFIRMACAO'",
                String.class, consultaEntregue.getId()).forEach(entregas::registrarEntrega);
        processamento.sincronizar(entregue.id());
        String entrouAntes = jdbc.queryForObject("SELECT entrou_em::text FROM entradas_fila WHERE encaminhamento_id = ?", String.class,
                doEntregue.getId());

        processamento.executarCiclo();

        assertThat(status(consultaSemEntrega.getId())).isEqualTo("AGUARDANDO_RESPOSTA");
        assertThat(status(consultaEntregue.getId())).isEqualTo("AGUARDANDO_RESPOSTA");
        assertThat(decisao(consultaSemEntrega.getId())).isEqualTo("SEM_CONTATO");
        assertThat(decisao(consultaEntregue.getId())).isEqualTo("MANTIDA_SEM_RESPOSTA");

        jdbc.update("DELETE FROM confirmacoes_de_presenca WHERE agendamento_id IN (?, ?)", consultaSemEntrega.getId(), consultaEntregue.getId());
        PoliticaDaFila liberar = new PoliticaDaFila(politica.prazoDeConfirmacao(), politica.avisoFinal(), AcaoSemResposta.LIBERAR, true,
                politica.prazoDaOferta(), politica.antecedenciaMinima(), politica.ofertasPorVaga(), politica.esperaSemCandidato());
        new ProcessarFilaUseCaseImpl(filaRepository, agendamentos, encaminhamentos, profissionais, dispararLembrete, notificacao, links,
                modelos, transacao, liberar, relogio, "http://localhost:3000").executarCiclo();

        assertThat(status(consultaSemEntrega.getId())).isEqualTo("AGUARDANDO_RESPOSTA");
        assertThat(decisao(consultaSemEntrega.getId())).isEqualTo("SEM_CONTATO");
        assertThat(status(consultaEntregue.getId())).isEqualTo("CANCELADO");
        assertThat(decisao(consultaEntregue.getId())).isEqualTo("LIBERADA");
        assertThat(encaminhamentos.buscarEncaminhamento(doEntregue.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusEncaminhamento.AGUARDANDO_AGENDAMENTO);
        assertThat(jdbc.queryForObject("SELECT status FROM entradas_fila WHERE encaminhamento_id = ?", String.class, doEntregue.getId()))
                .isEqualTo("AGUARDANDO");
        assertThat(jdbc.queryForObject("SELECT entrou_em::text FROM entradas_fila WHERE encaminhamento_id = ?", String.class,
                doEntregue.getId())).isEqualTo(entrouAntes);
        assertThat(jdbc.queryForObject("SELECT origem FROM vagas_liberadas WHERE agendamento_origem_id = ?", String.class,
                consultaEntregue.getId())).isEqualTo("SEM_CONFIRMACAO");

        Agendamento confirmada = fila.confirmarPresenca(recepcao, consultaSemEntrega.getId());

        assertThat(confirmada.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(decisao(consultaSemEntrega.getId())).isEqualTo("CONFIRMADA_NA_RECEPCAO");
    }

    @Test
    void ofertaInterrompidaAntesDoEnvioEhRetomadaDepoisDeReiniciar() {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();

        encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);
        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());
        processamento.executarCiclo();
        UUID oferta = ofertaUnica(bruno).id();
        jdbc.update("UPDATE ofertas_de_vaga SET status = 'PENDENTE', enviada_em = NULL, envio_tentado_em = NULL WHERE id = ?", oferta);

        relogio.em(instante.plusMinutes(2));
        new ProcessarFilaUseCaseImpl(filaRepository, agendamentos, encaminhamentos, profissionais, dispararLembrete, notificacao, links,
                modelos, transacao, politica, relogio, "http://localhost:3000").executarCiclo();

        assertThat(jdbc.queryForObject("SELECT status FROM ofertas_de_vaga WHERE id = ?", String.class, oferta)).isEqualTo("ENVIADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ofertas_de_vaga WHERE paciente_id = ?", Integer.class, bruno.id())).isEqualTo(1);
    }

    @Test
    void comAAutomacaoDesligadaNadaEhEnviadoSozinhoMasOPainelSeAtualiza() {
        ProcessarFilaUseCaseImpl semAutomacao = new ProcessarFilaUseCaseImpl(filaRepository, agendamentos, encaminhamentos, profissionais,
                dispararLembrete, notificacao, links, modelos, transacao, politica, relogio, "http://localhost:3000", false);
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();

        encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);
        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());

        assertThat(semAutomacao.atualizarSemEnvios()).isPositive();
        assertThat(semAutomacao.ofertarEmSeguida()).isZero();

        assertThat(jdbc.queryForObject("SELECT status FROM vagas_liberadas WHERE agendamento_origem_id = ?", String.class,
                consultaDaAna.getId())).isEqualTo("ABERTA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ofertas_de_vaga WHERE paciente_id = ?", Integer.class, bruno.id())).isZero();

        processamento.ofertarVagas();
        UUID oferta = ofertaUnica(bruno).id();
        relogio.em(AS_14.minus(politica.antecedenciaMinima()).plusSeconds(1));
        semAutomacao.atualizarSemEnvios();

        assertThat(jdbc.queryForObject("SELECT status FROM ofertas_de_vaga WHERE id = ?", String.class, oferta)).isEqualTo("EXPIRADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ofertas_de_vaga WHERE paciente_id <> ? AND vaga_id = "
                + "(SELECT vaga_id FROM ofertas_de_vaga WHERE id = ?)", Integer.class, bruno.id(), oferta)).isZero();
    }

    @Test
    void cadaPessoaVeApenasAsSuasOfertasEOEspecialistaApenasASuaArea() {
        Paciente ana = paciente();
        Agendamento consultaDaAna = agenda.agendar(recepcao, encaminhamento(ana.id(), especialidade, Prioridade.NAO_URGENTE).getId(),
                especialista.id(), AS_14, null);
        Paciente bruno = paciente();
        Encaminhamento doBruno = encaminhamento(bruno.id(), especialidade, Prioridade.URGENTE);
        Paciente carla = paciente();

        encaminhamento(carla.id(), especialidade, Prioridade.NAO_URGENTE);
        agenda.cancelar(recepcao, consultaDaAna.getId(), consultaDaAna.getVersao());
        processamento.executarCiclo();
        OfertaParaPaciente doBrunoOferta = ofertaUnica(bruno);

        assertThatThrownBy(() -> fila.aceitar(carla.ator(), doBrunoOferta.id())).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> fila.painel(carla.ator(), FiltroDaFila.todas(), PedidoDePagina.de(1, 10)))
                .isInstanceOf(AcessoNegadoException.class);

        Ator especialistaDaArea = Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, especialista.id());
        PainelDaFila painel = fila.painel(especialistaDaArea, new FiltroDaFila("CARDIOLOGIA", null, null), PedidoDePagina.de(1, 10));

        assertThat(painel.filtro().especialidade()).isEqualTo(especialidade);
        assertThat(painel.podeIntervir()).isFalse();
        assertThat(painel.fila().itens()).extracting(p -> p.pacienteId()).containsExactly(bruno.id(), ana.id(), carla.id());
        assertThat(painel.ofertas()).extracting(o -> o.pacienteId()).containsExactly(bruno.id());

        Profissional deOutraArea = especialistaComAgenda(novaEspecialidade());
        UUID entradaDoBruno = jdbc.queryForObject("SELECT id FROM entradas_fila WHERE encaminhamento_id = ?", UUID.class, doBruno.getId());

        assertThatThrownBy(() -> fila.detalhar(Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, deOutraArea.id()), entradaDoBruno))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> fila.pausar(especialistaDaArea, entradaDoBruno, "Sem contato")).isInstanceOf(AcessoNegadoException.class);

        fila.pausar(recepcao, entradaDoBruno, "Pessoa internada, aguardar alta");

        assertThat(filaDe(fila.minhaFila(bruno.ator()), doBruno).status()).isEqualTo(StatusEntrada.PAUSADA);
        assertThat(filaDe(fila.minhaFila(bruno.ator()), doBruno).posicao()).isNull();
        assertThat(fila.detalhar(recepcao, entradaDoBruno).movimentacoes()).extracting(m -> m.evento().name())
                .contains("ENTROU_NA_FILA", "OFERTA_CRIADA", "PAUSADA");
    }

    private record Paciente(UUID id, String telefone) {
        Ator ator() {
            return Ator.paciente(UUID.randomUUID(), id);
        }
    }

    private Paciente paciente() {
        String telefone = telefoneAleatorio();
        return new Paciente(cadastro.cadastrar(recepcao, dadosComTelefone("Paciente da Fila", cpfValido(), telefone)).paciente().id(),
                telefone);
    }

    private Encaminhamento encaminhamento(UUID paciente, String codigo, Prioridade prioridade) {
        instante = instante.plusMinutes(1);
        relogio.em(instante);

        Triagem t = triagens.iniciar(clinico, triagens.registrarChegada(recepcao, paciente).getId());
        Encaminhamento e = triagens.encaminhar(clinico, t.getId(),
                new NovoEncaminhamento(codigo, null, Modalidade.ELETIVO, prioridade, "Avaliação especializada"));
        Triagem avaliada = triagens.registrarAvaliacao(clinico, t.getId(), new AvaliacaoClinica("Dor persistente", null, null, null, null),
                new SinaisVitais(120, 80, 78, 16, new BigDecimal("36.7"), 98), prioridade, null, t.getVersao());

        triagens.finalizar(clinico, t.getId(), avaliada.getVersao());
        return encaminhamentos.buscarEncaminhamento(e.getId()).orElseThrow();
    }

    private OfertaParaPaciente ofertaUnica(Paciente p) {
        List<OfertaParaPaciente> ofertas = fila.minhaFila(p.ator()).ofertas();
        assertThat(ofertas).hasSize(1);
        return ofertas.getFirst();
    }

    private static PosicaoNaFila filaDe(MinhaFila minha, Encaminhamento e) {
        return minha.filas().stream().filter(f -> f.encaminhamentoId().equals(e.getId())).findFirst().orElseThrow();
    }

    private String status(UUID consulta) {
        return jdbc.queryForObject("SELECT status FROM agendamentos WHERE id = ?", String.class, consulta);
    }

    private String decisao(UUID consulta) {
        return jdbc.queryForList("SELECT decisao FROM confirmacoes_de_presenca WHERE agendamento_id = ?", String.class, consulta).stream()
                .findFirst().orElse(null);
    }

    private String novaEspecialidade() {
        String codigo = "TESTE_" + Long.toString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE), 36).toUpperCase();
        jdbc.update("INSERT INTO especialidades (codigo, nome, plantao) VALUES (?, ?, FALSE)", codigo, "Especialidade " + codigo);
        return codigo;
    }

    private Profissional especialistaComAgenda(String codigo) {
        Profissional p = profissional(TipoProfissional.ESPECIALISTA, codigo);
        profissionais.salvarDisponibilidade(new Disponibilidade(UUID.randomUUID(), p.id(), DayOfWeek.MONDAY,
                LocalTime.of(14, 0), LocalTime.of(15, 30), 30));
        return p;
    }

    private Profissional profissional(TipoProfissional tipo, String codigo) {
        Profissional p = new Profissional(UUID.randomUUID(), "Profissional " + tipo, "CRM-" + UUID.randomUUID().toString().substring(0, 12),
                tipo, codigo, "Policlínica Central", "Rua das Flores, 100", true);
        profissionais.salvarProfissional(p);
        return p;
    }
}
