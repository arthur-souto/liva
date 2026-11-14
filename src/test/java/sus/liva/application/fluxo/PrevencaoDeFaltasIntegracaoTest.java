package sus.liva.application.fluxo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import sus.liva.TestcontainersConfig;
import sus.liva.application.exception.AcessoNegadoException;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.AtendimentoEspecializadoUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.CadastroDePacienteUseCase;
import sus.liva.application.port.in.ConsultaEmRisco;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.MotivoContado;
import sus.liva.application.port.in.NovoEncaminhamento;
import sus.liva.application.port.in.PainelDeFaltas;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PrevencaoDeFaltasUseCase;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.in.TriagemUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.ausencia.MotivoDeAusencia;
import sus.liva.domain.model.ausencia.NivelDeRisco;
import sus.liva.domain.model.ausencia.ResultadoDaLigacao;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.Modalidade;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.cpfValido;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.dadosComTelefone;
import static sus.liva.application.fluxo.FluxoAssistencialIntegracaoTest.telefoneAleatorio;

@SpringBootTest(properties = {"confirmasus.whatsapp.provedor=console", "confirmasus.disparo-diario.cron=-",
        "confirmasus.fila.automacao.habilitada=false",
        "confirmasus.seguranca.api-key=chave-de-teste",
        "confirmasus.seguranca.jwt-segredo=segredo-de-teste-com-pelo-menos-32-caracteres"})
@Import({TestcontainersConfig.class, FluxoAssistencialIntegracaoTest.RelogioConfig.class})
class PrevencaoDeFaltasIntegracaoTest {
    private static final LocalDate SEGUNDA = LocalDate.of(2031, 3, 3);

    @Autowired private FluxoAssistencialIntegracaoTest.RelogioDeTeste relogio;
    @Autowired private CadastroDePacienteUseCase cadastro;
    @Autowired private TriagemUseCase triagens;
    @Autowired private AgendaDeConsultasUseCase agenda;
    @Autowired private AtendimentoEspecializadoUseCase atendimento;
    @Autowired private PrevencaoDeFaltasUseCase prevencao;
    @Autowired private ProcessarRespostaUseCase processarResposta;
    @Autowired private DispararLembreteUseCase dispararLembrete;
    @Autowired private ProfissionalRepositoryPort profissionais;
    @Autowired private AgendamentoRepositoryPort agendamentos;
    @Autowired private JdbcTemplate jdbc;

    private final Ator recepcao = Ator.equipe(UUID.randomUUID(), Papel.RECEPCAO);
    private Ator clinico;
    private Ator especialistaAtor;
    private String especialidade;
    private Profissional especialista;

    @BeforeEach
    void preparar() {
        relogio.em(SEGUNDA.atTime(8, 0));
        Profissional clinicoGeral = profissional(TipoProfissional.CLINICO_GERAL, null);
        clinico = Ator.profissional(UUID.randomUUID(), Papel.CLINICO_GERAL, clinicoGeral.id());
        especialidade = novaEspecialidade();
        especialista = profissional(TipoProfissional.ESPECIALISTA, especialidade);

        for (DayOfWeek dia : DayOfWeek.values())
            profissionais.salvarDisponibilidade(new Disponibilidade(UUID.randomUUID(), especialista.id(), dia,
                    LocalTime.of(7, 0), LocalTime.of(19, 0), 30));

        especialistaAtor = Ator.profissional(UUID.randomUUID(), Papel.ESPECIALISTA, especialista.id());
    }

    @Test
    void depoisDaFaltaAPessoaRecebeLinkParaRemarcarEOMotivoRespondidoPeloWhatsAppFicaRegistrado() {
        Paciente ana = paciente();
        Agendamento consulta = agendar(ana, SEGUNDA.atTime(9, 0));

        relogio.em(SEGUNDA.atTime(10, 0));

        atendimento.registrarFalta(especialistaAtor, consulta.getId(), versao(consulta.getId()));

        assertThat(motivo(consulta.getId())).containsEntry("ocorrencia", "FALTA").containsEntry("motivo", null);
        assertThat(jdbc.queryForObject("SELECT perguntado_em FROM motivos_de_ausencia WHERE agendamento_id = ?", Object.class,
                consulta.getId())).isNotNull();
        assertThat(textosEnviados(consulta.getId())).anyMatch(t -> t.contains("Sentimos sua falta") && t.contains("/acesso/link")
                && t.contains("1 - Esqueci da consulta"));

        ResultadoResposta resposta = processarResposta.processar(Telefone.de(ana.telefone()), "1", "motivo-" + consulta.getId());

        assertThat(resposta.agendamentoId()).isEqualTo(consulta.getId());
        assertThat(resposta.motivo()).isEqualTo("Motivo da ausência registrado");
        assertThat(motivo(consulta.getId())).containsEntry("motivo", "ESQUECI").containsEntry("canal", "WHATSAPP");
        assertThat(textosEnviados(consulta.getId())).anyMatch(t -> t.contains("Anotamos: esqueci da consulta"));

        ResultadoResposta depois = processarResposta.processar(Telefone.de(ana.telefone()), "2", "motivo-repetido-" + consulta.getId());

        assertThat(depois.motivo()).isNotEqualTo("Motivo da ausência registrado");
        assertThat(motivo(consulta.getId())).containsEntry("motivo", "ESQUECI");
    }

    @Test
    void quemCancelaPeloPedidoDeConfirmacaoTambemEhPerguntadoSobreOMotivo() {
        Paciente bruno = paciente();
        Agendamento consulta = agendar(bruno, SEGUNDA.atTime(16, 0));

        dispararLembrete.disparar(consulta.getId(), TipoLembrete.PEDIDO_CONFIRMACAO, true);

        processarResposta.processar(Telefone.de(bruno.telefone()), "2", "cancelar-" + consulta.getId());

        assertThat(status(consulta.getId())).isEqualTo("CANCELADO");
        assertThat(motivo(consulta.getId())).containsEntry("ocorrencia", "CANCELAMENTO").containsEntry("motivo", null);

        ResultadoResposta naoEntendida = processarResposta.processar(Telefone.de(bruno.telefone()), "tá complicado", "motivo-" + consulta.getId());

        assertThat(naoEntendida.motivo()).isEqualTo("Motivo da ausência não reconhecido");
        assertThat(motivo(consulta.getId())).containsEntry("motivo", null);
        assertThat(textosEnviados(consulta.getId())).anyMatch(t -> t.startsWith("Não entendi. Responda só com o número do motivo"));

        processarResposta.processar(Telefone.de(bruno.telefone()), "Sem dinheiro pro ônibus", "motivo-2-" + consulta.getId());

        assertThat(motivo(consulta.getId())).containsEntry("motivo", "TRANSPORTE").containsEntry("canal", "WHATSAPP");
    }

    @Test
    void aEquipeVeORiscoDasProximasConsultasComOsMotivosEApenasOProprioPacienteInformaOMotivoPeloPortal() {
        Paciente carla = paciente();
        Encaminhamento daCarla = encaminhamento(carla.id());
        Agendamento faltou = agenda.agendar(recepcao, daCarla.getId(), especialista.id(), SEGUNDA.atTime(9, 30), null);

        relogio.em(SEGUNDA.atTime(11, 0));
        atendimento.registrarFalta(especialistaAtor, faltou.getId(), versao(faltou.getId()));
        Agendamento proxima = agenda.agendar(recepcao, daCarla.getId(), especialista.id(), SEGUNDA.plusDays(2).atTime(10, 0), null);
        dispararLembrete.disparar(proxima.getId(), TipoLembrete.PEDIDO_CONFIRMACAO, true);

        Paciente davi = paciente();
        Agendamento confirmada = agendar(davi, SEGUNDA.plusDays(2).atTime(11, 0));

        dispararLembrete.disparar(confirmada.getId(), TipoLembrete.PEDIDO_CONFIRMACAO, true);
        processarResposta.processar(Telefone.de(davi.telefone()), "1", "confirmar-" + confirmada.getId());

        PainelDeFaltas painel = prevencao.painel(recepcao, 4);

        ConsultaEmRisco daCarla2 = painel.emRisco().stream().filter(c -> c.agendamentoId().equals(proxima.getId())).findFirst().orElseThrow();
        ConsultaEmRisco doDavi = painel.emRisco().stream().filter(c -> c.agendamentoId().equals(confirmada.getId())).findFirst().orElseThrow();

        assertThat(daCarla2.risco().nivel()).isEqualTo(NivelDeRisco.ALTO);
        assertThat(daCarla2.risco().fatores()).contains("Faltou 1 vez nos últimos 12 meses", "Não respondeu ao pedido de confirmação",
                "Primeira consulta na unidade");
        assertThat(doDavi.risco().nivel()).isEqualTo(NivelDeRisco.BAIXO);
        assertThat(doDavi.risco().fatores()).contains("Confirmou presença");
        assertThat(painel.emRisco().indexOf(daCarla2)).isLessThan(painel.emRisco().indexOf(doDavi));
        assertThat(painel.semanas()).hasSize(4);
        assertThat(painel.semanas().getLast().inicio()).isEqualTo(SEGUNDA.minusWeeks(1));
        assertThat(painel.especialidades()).anyMatch(e -> e.codigo().equals(especialidade) && e.faltaram() == 1);

        assertThatThrownBy(() -> prevencao.registrarMotivo(davi.ator(), faltou.getId(), MotivoDeAusencia.TRABALHO))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> prevencao.painel(especialistaAtor, 4)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> prevencao.registrarMotivo(carla.ator(), proxima.getId(), MotivoDeAusencia.TRABALHO))
                .isInstanceOf(IllegalStateException.class);

        long antes = trabalho(prevencao.painel(recepcao, 4));
        prevencao.registrarMotivo(carla.ator(), faltou.getId(), MotivoDeAusencia.TRABALHO);

        assertThat(motivo(faltou.getId())).containsEntry("motivo", "TRABALHO").containsEntry("canal", "PORTAL");
        assertThat(trabalho(prevencao.painel(recepcao, 4))).isEqualTo(antes + 1);

        relogio.em(SEGUNDA.plusWeeks(1).atTime(8, 0));
        PainelDeFaltas semanaSeguinte = prevencao.painel(recepcao, 4);

        assertThat(semanaSeguinte.semanas().getLast().inicio()).isEqualTo(SEGUNDA);
        assertThat(semanaSeguinte.semanas().getLast().faltaram()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void aLigacaoDaEquipeConfirmaAPresencaOuAumentaORiscoQuandoAPessoaNaoAtende() {
        Paciente elisa = paciente();
        Agendamento daElisa = agendar(elisa, SEGUNDA.plusDays(3).atTime(9, 0));
        Paciente fabio = paciente();
        Agendamento doFabio = agendar(fabio, SEGUNDA.plusDays(3).atTime(10, 0));
        int antes = emRisco(doFabio.getId()).risco().pontos();

        prevencao.registrarLigacao(recepcao, daElisa.getId(), ResultadoDaLigacao.CONFIRMOU);
        prevencao.registrarLigacao(recepcao, doFabio.getId(), ResultadoDaLigacao.NAO_ATENDEU);

        assertThat(status(daElisa.getId())).isEqualTo("CONFIRMADO");

        ConsultaEmRisco confirmada = emRisco(daElisa.getId());

        assertThat(confirmada.risco().fatores()).contains("Confirmou presença");
        assertThat(confirmada.ultimaLigacao().resultado()).isEqualTo(ResultadoDaLigacao.CONFIRMOU);

        ConsultaEmRisco naoAtendeu = emRisco(doFabio.getId());

        assertThat(naoAtendeu.risco().pontos()).isEqualTo(antes + 10);
        assertThat(naoAtendeu.risco().fatores()).contains("Não atendeu a ligação da unidade");

        relogio.em(SEGUNDA.atTime(8, 30));
        prevencao.registrarLigacao(recepcao, daElisa.getId(), ResultadoDaLigacao.CONFIRMOU);
        prevencao.registrarLigacao(recepcao, doFabio.getId(), ResultadoDaLigacao.VAI_REMARCAR);

        assertThat(emRisco(doFabio.getId()).ultimaLigacao().resultado()).isEqualTo(ResultadoDaLigacao.VAI_REMARCAR);
        assertThat(emRisco(doFabio.getId()).risco().fatores()).doesNotContain("Não atendeu a ligação da unidade");

        assertThatThrownBy(() -> prevencao.registrarLigacao(especialistaAtor, doFabio.getId(), ResultadoDaLigacao.CONFIRMOU))
                .isInstanceOf(AcessoNegadoException.class);

        relogio.em(SEGUNDA.plusDays(4).atTime(8, 0));

        assertThatThrownBy(() -> prevencao.registrarLigacao(recepcao, doFabio.getId(), ResultadoDaLigacao.CONFIRMOU))
                .isInstanceOf(IllegalStateException.class);
    }

    private ConsultaEmRisco emRisco(UUID consulta) {
        return prevencao.painel(recepcao, 4).emRisco().stream().filter(c -> c.agendamentoId().equals(consulta)).findFirst().orElseThrow();
    }

    private static long trabalho(PainelDeFaltas painel) {
        return painel.motivos().stream().filter(m -> m.motivo() == MotivoDeAusencia.TRABALHO).mapToLong(MotivoContado::faltas).sum();
    }

    private record Paciente(UUID id, String telefone) {
        Ator ator() {
            return Ator.paciente(UUID.randomUUID(), id);
        }
    }

    private Paciente paciente() {
        String telefone = telefoneAleatorio();
        return new Paciente(cadastro.cadastrar(recepcao, dadosComTelefone("Paciente Pontual", cpfValido(), telefone)).paciente().id(),
                telefone);
    }

    private Agendamento agendar(Paciente p, LocalDateTime inicio) {
        return agenda.agendar(recepcao, encaminhamento(p.id()).getId(), especialista.id(), inicio, null);
    }

    private Encaminhamento encaminhamento(UUID paciente) {
        Triagem t = triagens.iniciar(clinico, triagens.registrarChegada(recepcao, paciente).getId());
        Encaminhamento e = triagens.encaminhar(clinico, t.getId(),
                new NovoEncaminhamento(especialidade, null, Modalidade.ELETIVO, Prioridade.POUCO_URGENTE, "Acompanhamento"));
        Triagem avaliada = triagens.registrarAvaliacao(clinico, t.getId(), new AvaliacaoClinica("Dor persistente", null, null, null, null),
                new SinaisVitais(120, 80, 78, 16, new BigDecimal("36.7"), 98), Prioridade.POUCO_URGENTE, null, t.getVersao());

        triagens.finalizar(clinico, t.getId(), avaliada.getVersao());
        return e;
    }

    private long versao(UUID consulta) {
        return agendamentos.buscarAgendamento(consulta).orElseThrow().getVersao();
    }

    private String status(UUID consulta) {
        return jdbc.queryForObject("SELECT status FROM agendamentos WHERE id = ?", String.class, consulta);
    }

    private Map<String, Object> motivo(UUID consulta) {
        return jdbc.queryForMap("SELECT ocorrencia, motivo, canal FROM motivos_de_ausencia WHERE agendamento_id = ?", consulta);
    }

    private List<String> textosEnviados(UUID consulta) {
        return jdbc.queryForList("SELECT texto FROM registros_mensagem WHERE agendamento_id = ? ORDER BY id", String.class, consulta);
    }

    private String novaEspecialidade() {
        String codigo = "FALTAS_" + Long.toString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE), 36).toUpperCase();
        jdbc.update("INSERT INTO especialidades (codigo, nome, plantao) VALUES (?, ?, FALSE)", codigo, "Especialidade " + codigo);
        return codigo;
    }

    private Profissional profissional(TipoProfissional tipo, String codigo) {
        Profissional p = new Profissional(UUID.randomUUID(), "Profissional " + tipo, "CRM-" + UUID.randomUUID().toString().substring(0, 12),
                tipo, codigo, "Policlínica Central", "Rua das Flores, 100", true);
        profissionais.salvarProfissional(p);
        return p;
    }
}
