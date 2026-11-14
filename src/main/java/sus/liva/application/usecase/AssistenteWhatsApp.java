package sus.liva.application.usecase;

import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.HorarioDisponivel;
import sus.liva.application.port.in.RemarcacoesDoMes;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.ContaDeAcesso;
import sus.liva.application.port.out.ContaDeAcessoPort;
import sus.liva.application.port.out.Conversa;
import sus.liva.application.port.out.ConversaPort;
import sus.liva.application.port.out.CuidadorRepositoryPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.EtapaDaConversa;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.cuidador.Cuidador;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.profissional.Especialidade;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class AssistenteWhatsApp {
    private static final Duration EXPIRACAO = Duration.ofMinutes(30);
    private static final Duration INTERVALO_SEM_CADASTRO = Duration.ofMinutes(10);
    private static final int DIAS_DE_BUSCA = 14;
    private static final int HORARIOS_POR_DIA = 2;
    private static final int HORARIOS_SUGERIDOS = 6;
    private static final Set<String> PEDIDOS_DE_MENU = Set.of("menu", "oi", "ola", "inicio", "opcoes", "ajuda", "bom dia", "boa tarde", "boa noite");
    private static final Set<String> PEDIDOS_DE_SAIDA = Set.of("0", "sair", "tchau", "obrigado", "obrigada");

    private final PacienteRepositoryPort pacientes;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final AgendamentoRepositoryPort agendamentos;
    private final ProfissionalRepositoryPort profissionais;
    private final ContaDeAcessoPort contas;
    private final ConversaPort conversas;
    private final RegistroMensagemPort registros;
    private final AgendaDeConsultasUseCase agenda;
    private final LinksDeAcesso links;
    private final LimiteDeRemarcacoes limite;
    private final NotificacaoAoPaciente notificacao;
    private final ModeloMensagens modelos;
    private final CuidadorRepositoryPort cuidadores;
    private final Clock clock;

    public AssistenteWhatsApp(PacienteRepositoryPort pacientes, EncaminhamentoRepositoryPort encaminhamentos,
                              AgendamentoRepositoryPort agendamentos, ProfissionalRepositoryPort profissionais,
                              ContaDeAcessoPort contas, ConversaPort conversas, RegistroMensagemPort registros,
                              AgendaDeConsultasUseCase agenda, LinksDeAcesso links, LimiteDeRemarcacoes limite,
                              NotificacaoAoPaciente notificacao, ModeloMensagens modelos, CuidadorRepositoryPort cuidadores,
                              Clock clock) {
        this.pacientes = pacientes;
        this.encaminhamentos = encaminhamentos;
        this.agendamentos = agendamentos;
        this.profissionais = profissionais;
        this.contas = contas;
        this.conversas = conversas;
        this.registros = registros;
        this.agenda = agenda;
        this.links = links;
        this.limite = limite;
        this.notificacao = notificacao;
        this.modelos = modelos;
        this.cuidadores = cuidadores;
        this.clock = clock;
    }

    public boolean conversaAtiva(Telefone telefone) {
        return ativa(telefone).isPresent();
    }

    public boolean pedidoDeMenu(String texto) {
        return PEDIDOS_DE_MENU.contains(normalizar(texto));
    }

    public boolean podeRemarcar(UUID pacienteId) {
        return limite.doMes(pacienteId).restantes() > 0;
    }

    public void avisarLimite(Telefone destino, Paciente paciente) {
        notificacao.enviar(null, destino, modelos.limiteDeRemarcacoes(paciente, LimiteDeRemarcacoes.POR_MES));
    }

    public String retornoDePedidoDeOutroHorario(Paciente paciente, Agendamento consulta) {
        limite.registrar(paciente.id(), consulta.getId(), null, LimiteDeRemarcacoes.WHATSAPP);
        String link = links.gerar(paciente.id(), "/portal/agendar/" + consulta.getEncaminhamentoId());
        return modelos.linkDeAgendamento(paciente, link, LinksDeAcesso.VALIDADE.toMinutes());
    }

    public ResultadoResposta atender(Telefone remetente, String texto) {
        String resposta = normalizar(texto);
        Optional<Conversa> conversa = ativa(remetente);
        if (conversa.isEmpty() || PEDIDOS_DE_MENU.contains(resposta))
            return iniciar(remetente, conversa.map(Conversa::pacienteId).orElse(null));
        if (PEDIDOS_DE_SAIDA.contains(resposta)) {
            conversas.encerrarConversa(remetente);
            return responder("SAIR", null, remetente, modelos.despedida());
        }

        Conversa atual = conversa.get();
        Optional<Paciente> paciente = Optional.ofNullable(atual.pacienteId()).flatMap(pacientes::buscarPaciente);
        if (atual.etapa() == EtapaDaConversa.ESCOLHER_PACIENTE) return escolherPaciente(remetente, atual, resposta);
        if (paciente.isEmpty()) return iniciar(remetente, null);

        return switch (atual.etapa()) {
            case MENU -> menu(remetente, paciente.get(), resposta);
            case ESCOLHER_ENCAMINHAMENTO -> escolha(atual, resposta)
                    .flatMap(id -> encaminhamentos.buscarEncaminhamento(UUID.fromString(id)))
                    .map(e -> horarios(remetente, paciente.get(), e))
                    .orElseGet(() -> naoEntendi(remetente, atual));
            case ESCOLHER_HORARIO -> escolha(atual, resposta)
                    .map(opcao -> agendar(remetente, paciente.get(), opcao))
                    .orElseGet(() -> naoEntendi(remetente, atual));
            case ESCOLHER_CONSULTA_REMARCAR -> escolha(atual, resposta)
                    .flatMap(id -> agendamentos.buscarAgendamento(UUID.fromString(id)))
                    .map(a -> enviarLinkDeRemarcacao(remetente, paciente.get(), a))
                    .orElseGet(() -> naoEntendi(remetente, atual));
            case ESCOLHER_CONSULTA_CANCELAR -> escolha(atual, resposta)
                    .flatMap(id -> agendamentos.buscarAgendamento(UUID.fromString(id)))
                    .map(a -> confirmarCancelamento(remetente, paciente.get(), a))
                    .orElseGet(() -> naoEntendi(remetente, atual));
            case CONFIRMAR_CANCELAMENTO -> concluirCancelamento(remetente, paciente.get(), atual, resposta);
            case ESCOLHER_PACIENTE -> escolherPaciente(remetente, atual, resposta);
        };
    }

    private ResultadoResposta iniciar(Telefone remetente, UUID pacienteAtual) {
        List<Paciente> doTelefone = pacientesDoTelefone(remetente);
        if (doTelefone.isEmpty()) {
            conversas.encerrarConversa(remetente);
            String texto = modelos.semCadastro();
            if (registros.enviadaDesde(remetente, texto, Instant.now(clock).minus(INTERVALO_SEM_CADASTRO)))
                return ResultadoResposta.ignorada("Telefone sem cadastro; aviso já enviado há pouco");
            return responder("SEM_CADASTRO", null, remetente, texto);
        }
        Optional<Paciente> escolhido = doTelefone.stream().filter(p -> p.id().equals(pacienteAtual)).findFirst();
        if (escolhido.isEmpty() && doTelefone.size() == 1) escolhido = Optional.of(doTelefone.getFirst());
        if (escolhido.isPresent()) return mostrarMenu(remetente, escolhido.get(), menuDe(remetente, escolhido.get()));

        salvar(remetente, null, EtapaDaConversa.ESCOLHER_PACIENTE, doTelefone.stream().map(p -> p.id().toString()).toList());
        return responder("ESCOLHER_PACIENTE", null, remetente, modelos.escolherPaciente(doTelefone));
    }

    private ResultadoResposta escolherPaciente(Telefone remetente, Conversa atual, String resposta) {
        return escolha(atual, resposta)
                .flatMap(id -> pacientes.buscarPaciente(UUID.fromString(id)))
                .map(p -> mostrarMenu(remetente, p, menuDe(remetente, p)))
                .orElseGet(() -> naoEntendi(remetente, atual));
    }

    private ResultadoResposta mostrarMenu(Telefone remetente, Paciente paciente, String texto) {
        salvar(remetente, paciente.id(), EtapaDaConversa.MENU, List.of());
        return responder("MENU", null, remetente, texto);
    }

    private ResultadoResposta menu(Telefone remetente, Paciente paciente, String resposta) {
        return switch (resposta) {
            case "1", "agendar", "agendar consulta" -> paraAgendar(remetente, paciente);
            case "2", "consultas", "minhas consultas", "ver minhas consultas" -> minhasConsultas(remetente, paciente);
            case "3", "remarcar", "remarcar consulta" -> paraRemarcar(remetente, paciente);
            case "4", "cancelar", "cancelar consulta" -> paraCancelar(remetente, paciente);
            default -> mostrarMenu(remetente, paciente, modelos.naoEntendiOpcao() + "\n\n" + menuDe(remetente, paciente));
        };
    }

    private ResultadoResposta paraAgendar(Telefone remetente, Paciente paciente) {
        List<Encaminhamento> liberados = encaminhamentos.encaminhamentosDoPaciente(paciente.id()).stream()
                .filter(Encaminhamento::permiteAgendamento)
                .sorted(Comparator.comparing(Encaminhamento::getCriadoEm))
                .toList();
        if (liberados.isEmpty())
            return mostrarMenu(remetente, paciente, """
                    Você não tem encaminhamentos liberados para agendamento agora.
                    Quando a equipe da unidade liberar, você recebe um aviso por aqui.

                    Envie MENU para ver as opções.""");
        if (liberados.size() == 1) return horarios(remetente, paciente, liberados.getFirst());

        salvar(remetente, paciente.id(), EtapaDaConversa.ESCOLHER_ENCAMINHAMENTO, liberados.stream().map(e -> e.getId().toString()).toList());
        return responder("ESCOLHER_ENCAMINHAMENTO", null, remetente, modelos.lista("Qual especialidade você quer agendar?",
                liberados.stream().map(e -> nomeDaEspecialidade(e.getEspecialidade())).toList(), "Responda com o número."));
    }

    private ResultadoResposta horarios(Telefone remetente, Paciente paciente, Encaminhamento encaminhamento) {
        String especialidade = nomeDaEspecialidade(encaminhamento.getEspecialidade());
        String link = links.gerar(paciente.id(), "/portal/agendar/" + encaminhamento.getId());
        LocalDate hoje = LocalDate.now(clock);
        List<HorarioDisponivel> sugeridos = sugerir(agenda.horariosDisponiveis(ator(remetente, paciente), encaminhamento.getId(), hoje,
                hoje.plusDays(DIAS_DE_BUSCA - 1)));
        if (sugeridos.isEmpty()) {
            conversas.encerrarConversa(remetente);
            return responder("SEM_HORARIOS", null, remetente, """
                    Não há horários livres de %s nas próximas duas semanas.
                    Veja outras datas pelo link (vale por %d minutos):
                    %s""".formatted(especialidade, LinksDeAcesso.VALIDADE.toMinutes(), link));
        }
        salvar(remetente, paciente.id(), EtapaDaConversa.ESCOLHER_HORARIO, sugeridos.stream()
                .map(h -> encaminhamento.getId() + "|" + h.profissionalId() + "|" + h.inicio()).toList());
        return responder("ESCOLHER_HORARIO", null, remetente, modelos.lista("Horários livres para " + especialidade + ":",
                sugeridos.stream().map(h -> modelos.quando(h.inicio()) + " com " + h.nomeProfissional()).toList(),
                "Responda com o número do horário.\nPrefere outro dia? Escolha pelo link (vale por %d minutos):\n%s"
                        .formatted(LinksDeAcesso.VALIDADE.toMinutes(), link)));
    }

    private ResultadoResposta agendar(Telefone remetente, Paciente paciente, String opcao) {
        String[] partes = opcao.split("\\|");
        UUID encaminhamentoId = UUID.fromString(partes[0]);
        try {
            Agendamento consulta = agenda.agendar(ator(remetente, paciente), encaminhamentoId, UUID.fromString(partes[1]),
                    LocalDateTime.parse(partes[2]), UUID.randomUUID().toString());
            conversas.encerrarConversa(remetente);
            return ResultadoResposta.autoatendimento("AGENDADO", consulta.getId(), true);
        } catch (RuntimeException e) {
            notificacao.enviar(null, remetente, "Esse horário não está mais disponível. Veja as opções atualizadas:");
            return encaminhamentos.buscarEncaminhamento(encaminhamentoId)
                    .map(enc -> horarios(remetente, paciente, enc))
                    .orElseGet(() -> mostrarMenu(remetente, paciente, menuDe(remetente, paciente)));
        }
    }

    private ResultadoResposta minhasConsultas(Telefone remetente, Paciente paciente) {
        List<Agendamento> futuras = futuras(paciente);
        if (futuras.isEmpty())
            return mostrarMenu(remetente, paciente, "Você não tem consultas marcadas.\n\nEnvie 1 para agendar ou MENU para ver as opções.");
        return mostrarMenu(remetente, paciente, modelos.lista("Suas próximas consultas:",
                futuras.stream().map(this::descrever).toList(), "\nEnvie 3 para remarcar, 4 para cancelar ou MENU para ver as opções."));
    }

    private ResultadoResposta paraRemarcar(Telefone remetente, Paciente paciente) {
        List<Agendamento> remarcaveis = futuras(paciente).stream().filter(Agendamento::doEncaminhamento).toList();
        if (remarcaveis.isEmpty())
            return mostrarMenu(remetente, paciente, "Você não tem consultas que possam ser remarcadas.\n\nEnvie MENU para ver as opções.");
        if (!podeRemarcar(paciente.id())) {
            conversas.encerrarConversa(remetente);
            return responder("LIMITE_DE_REMARCACOES", null, remetente, modelos.limiteDeRemarcacoes(paciente, LimiteDeRemarcacoes.POR_MES));
        }
        if (remarcaveis.size() == 1) return enviarLinkDeRemarcacao(remetente, paciente, remarcaveis.getFirst());

        salvar(remetente, paciente.id(), EtapaDaConversa.ESCOLHER_CONSULTA_REMARCAR, remarcaveis.stream().map(a -> a.getId().toString()).toList());
        return responder("ESCOLHER_CONSULTA_REMARCAR", null, remetente, modelos.lista("Qual consulta você quer remarcar?",
                remarcaveis.stream().map(this::descrever).toList(), "Responda com o número."));
    }

    private ResultadoResposta enviarLinkDeRemarcacao(Telefone remetente, Paciente paciente, Agendamento consulta) {
        conversas.encerrarConversa(remetente);
        RemarcacoesDoMes doMes = limite.doMes(paciente.id());
        if (doMes.restantes() == 0)
            return responder("LIMITE_DE_REMARCACOES", consulta.getId(), remetente, modelos.limiteDeRemarcacoes(paciente, LimiteDeRemarcacoes.POR_MES));
        String link = links.gerar(paciente.id(), "/portal/agendar/%s?remarcar=%s&versao=%d"
                .formatted(consulta.getEncaminhamentoId(), consulta.getId(), consulta.getVersao() == null ? 0 : consulta.getVersao()));
        return responder("LINK_DE_REMARCACAO", consulta.getId(), remetente,
                modelos.linkDeRemarcacao(paciente, consulta, link, doMes.restantes(), LinksDeAcesso.VALIDADE.toMinutes()));
    }

    private ResultadoResposta paraCancelar(Telefone remetente, Paciente paciente) {
        List<Agendamento> futuras = futuras(paciente);
        if (futuras.isEmpty())
            return mostrarMenu(remetente, paciente, "Você não tem consultas marcadas para cancelar.\n\nEnvie MENU para ver as opções.");
        if (futuras.size() == 1) return confirmarCancelamento(remetente, paciente, futuras.getFirst());

        salvar(remetente, paciente.id(), EtapaDaConversa.ESCOLHER_CONSULTA_CANCELAR, futuras.stream().map(a -> a.getId().toString()).toList());
        return responder("ESCOLHER_CONSULTA_CANCELAR", null, remetente, modelos.lista("Qual consulta você quer cancelar?",
                futuras.stream().map(this::descrever).toList(), "Responda com o número."));
    }

    private ResultadoResposta confirmarCancelamento(Telefone remetente, Paciente paciente, Agendamento consulta) {
        salvar(remetente, paciente.id(), EtapaDaConversa.CONFIRMAR_CANCELAMENTO, List.of(consulta.getId().toString()));
        return responder("CONFIRMAR_CANCELAMENTO", consulta.getId(), remetente, modelos.confirmarCancelamento(descrever(consulta)));
    }

    private ResultadoResposta concluirCancelamento(Telefone remetente, Paciente paciente, Conversa atual, String resposta) {
        UUID consultaId = UUID.fromString(atual.opcoes().getFirst());
        if (Stream.of("2", "nao", "manter").anyMatch(resposta::equals)) {
            conversas.encerrarConversa(remetente);
            return responder("CANCELAMENTO_DESISTIDO", consultaId, remetente, "Certo, sua consulta continua marcada.");
        }
        if (Stream.of("1", "sim", "cancelar").noneMatch(resposta::equals)) return naoEntendi(remetente, atual);
        conversas.encerrarConversa(remetente);
        try {
            Agendamento atualizada = agendamentos.buscarAgendamento(consultaId).orElseThrow();
            Agendamento cancelada = agenda.cancelar(ator(remetente, paciente), consultaId, atualizada.getVersao() == null ? 0 : atualizada.getVersao());
            return ResultadoResposta.autoatendimento("CANCELADO", cancelada.getId(), true);
        } catch (RuntimeException e) {
            return responder("CANCELAMENTO_FALHOU", consultaId, remetente,
                    "Não foi possível cancelar esta consulta agora. Procure a recepção da unidade.");
        }
    }

    private ResultadoResposta naoEntendi(Telefone remetente, Conversa atual) {
        salvar(remetente, atual.pacienteId(), atual.etapa(), atual.opcoes());
        return responder("NAO_ENTENDI", null, remetente, modelos.naoEntendiOpcao());
    }

    private Optional<String> escolha(Conversa conversa, String resposta) {
        if (!resposta.matches("\\d{1,2}")) return Optional.empty();
        int indice = Integer.parseInt(resposta) - 1;
        if (indice < 0 || indice >= conversa.opcoes().size()) return Optional.empty();
        return Optional.of(conversa.opcoes().get(indice));
    }

    private List<HorarioDisponivel> sugerir(List<HorarioDisponivel> livres) {
        Map<LocalDate, List<HorarioDisponivel>> porDia = new LinkedHashMap<>();
        for (HorarioDisponivel h : livres) {
            List<HorarioDisponivel> doDia = porDia.computeIfAbsent(h.inicio().toLocalDate(), d -> new ArrayList<>());
            if (doDia.size() < HORARIOS_POR_DIA) doDia.add(h);
        }
        return porDia.values().stream().flatMap(List::stream).limit(HORARIOS_SUGERIDOS).toList();
    }

    private List<Agendamento> futuras(Paciente paciente) {
        LocalDateTime agora = LocalDateTime.now(clock);
        return agendamentos.consultasDoPaciente(paciente.id()).stream()
                .filter(a -> a.getStatus().ocupaAgenda() && a.getStatus() != StatusAgendamento.EM_ANDAMENTO)
                .filter(a -> a.getDataHora().isAfter(agora))
                .sorted(Comparator.comparing(Agendamento::getDataHora))
                .toList();
    }

    private String descrever(Agendamento a) {
        String especialidade = Optional.ofNullable(a.getEncaminhamentoId())
                .flatMap(encaminhamentos::buscarEncaminhamento)
                .map(e -> nomeDaEspecialidade(e.getEspecialidade()))
                .orElse("Atendimento");
        String profissional = Optional.ofNullable(a.getProfissionalId())
                .flatMap(profissionais::buscarProfissional)
                .map(p -> " com " + p.nome())
                .orElse("");
        return "%s em %s%s".formatted(especialidade, modelos.quando(a), profissional);
    }

    private String nomeDaEspecialidade(String codigo) {
        return profissionais.buscarEspecialidade(codigo).map(Especialidade::nome).orElse(codigo);
    }

    private List<Paciente> pacientesDoTelefone(Telefone telefone) {
        Stream<Paciente> proprios = telefones(telefone).flatMap(t -> pacientes.buscarPorTelefone(t).stream()).filter(Paciente::consentimento);
        Stream<Paciente> dependentes = cuidadoresDoTelefone(telefone)
                .flatMap(c -> cuidadores.dependentesDe(c.id()).stream())
                .flatMap(id -> pacientes.buscarPaciente(id).stream());
        return Stream.concat(proprios, dependentes).distinct().toList();
    }

    private Stream<Telefone> telefones(Telefone telefone) {
        return Stream.concat(Stream.of(telefone), alternativo(telefone).stream());
    }

    private Stream<Cuidador> cuidadoresDoTelefone(Telefone telefone) {
        return telefones(telefone).flatMap(t -> cuidadores.buscarCuidadoresPorTelefone(t).stream()).distinct();
    }

    private Optional<Cuidador> cuidadorFalando(Telefone remetente, Paciente paciente) {
        if (paciente.telefone() != null && paciente.telefone().chaveComparacao().equals(remetente.chaveComparacao())) return Optional.empty();
        return cuidadoresDoTelefone(remetente).filter(c -> cuidadores.cuidaDe(c.id(), paciente.id())).findFirst();
    }

    private String menuDe(Telefone remetente, Paciente paciente) {
        return cuidadorFalando(remetente, paciente)
                .map(c -> modelos.menuParaCuidador(c.nome(), paciente))
                .orElseGet(() -> modelos.menu(paciente));
    }

    private static Optional<Telefone> alternativo(Telefone telefone) {
        String e164 = telefone.e164();
        try {
            if (e164.length() == 14 && e164.charAt(5) == '9') return Optional.of(new Telefone(e164.substring(0, 5) + e164.substring(6)));
            if (e164.length() == 13) return Optional.of(new Telefone(e164.substring(0, 5) + "9" + e164.substring(5)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Ator ator(Telefone remetente, Paciente paciente) {
        Optional<Cuidador> cuidador = cuidadorFalando(remetente, paciente);
        if (cuidador.isPresent()) return Ator.cuidador(cuidador.get().id(), paciente.id());
        return contas.buscarPorPaciente(paciente.id()).map(ContaDeAcesso::ator).orElse(Ator.paciente(paciente.id(), paciente.id()));
    }

    private Optional<Conversa> ativa(Telefone telefone) {
        Instant limiteDeAtividade = Instant.now(clock).minus(EXPIRACAO);
        return conversas.buscarConversa(telefone).filter(c -> c.atualizadaEm().isAfter(limiteDeAtividade));
    }

    private void salvar(Telefone telefone, UUID pacienteId, EtapaDaConversa etapa, List<String> opcoes) {
        conversas.salvarConversa(new Conversa(telefone, pacienteId, etapa, opcoes, Instant.now(clock)));
    }

    private ResultadoResposta responder(String etapa, UUID agendamentoId, Telefone destino, String texto) {
        return ResultadoResposta.autoatendimento(etapa, agendamentoId, notificacao.enviar(agendamentoId, destino, texto));
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
