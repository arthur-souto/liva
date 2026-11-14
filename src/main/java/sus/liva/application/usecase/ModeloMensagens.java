package sus.liva.application.usecase;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.ausencia.MotivoDeAusencia;
import sus.liva.domain.model.fila.AcaoSemResposta;
import sus.liva.domain.model.paciente.Paciente;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ModeloMensagens {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM (EEEE)", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String MAPA = "https://www.google.com/maps/search/?api=1&query=";
    private static final String AGENDA = "https://calendar.google.com/calendar/render?action=TEMPLATE";
    private static final DateTimeFormatter AGENDA_HORARIO = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final int DURACAO_PADRAO = 30;

    private final Duration prazoDeConfirmacao;
    private final AcaoSemResposta semResposta;

    public ModeloMensagens() {
        this(Duration.ofHours(24), AcaoSemResposta.MANTER);
    }

    public ModeloMensagens(Duration prazoDeConfirmacao, AcaoSemResposta semResposta) {
        this.prazoDeConfirmacao = prazoDeConfirmacao;
        this.semResposta = semResposta;
    }

    public String montar(TipoLembrete tipo, Paciente p, Agendamento a) {
        String data = a.getDataHora().format(DATA);
        String hora = a.getDataHora().format(HORA);
        return switch (tipo) {
            case CONFIRMACAO_AGENDAMENTO -> """
                Olá, %s! Seu atendimento na %s foi agendado para %s, às %s.
                Endereço: %s
                Como chegar: %s
                Salvar na agenda do celular: %s
                Leve seu cartão do SUS e um documento com foto.
                Vamos te lembrar alguns dias antes.""".formatted(
                    p.nomeCompleto(), a.getUnidade(), data, hora, a.getEndereco(), comoChegar(a), salvarNaAgenda(a));
            case PEDIDO_CONFIRMACAO -> """
                Olá, %s! Você tem atendimento na %s em %s, às %s.
                Você consegue ir?
                1 - Sim, vou
                2 - Não vou conseguir
                3 - Quero outro horário
                Responda com o número ou por escrito até %s.""".formatted(
                    p.nomeCompleto(), a.getUnidade(), data, hora, quando(a.getDataHora().minus(prazoDeConfirmacao)));
            case LEMBRETE_VESPERA -> {
                String t = "Lembrete: amanhã, %s, às %s, você tem atendimento na %s.\nEndereço: %s\nComo chegar: %s"
                        .formatted(data, hora, a.getUnidade(), a.getEndereco(), comoChegar(a));
                if (a.getPreparo() != null && !a.getPreparo().isBlank()) t += "\nPreparo: " + a.getPreparo();
                yield t + "\nSe não puder ir, responda 2 para liberar a vaga para outra pessoa.";
            }
            case AVISO_FINAL_CONFIRMACAO -> """
                Olá, %s! Ainda não recebemos a sua resposta sobre o atendimento na %s em %s, às %s.
                Você consegue ir?
                1 - Sim, vou
                2 - Não vou conseguir
                3 - Quero outro horário
                Responda até %s. %s""".formatted(
                    primeiroNome(p), a.getUnidade(), data, hora, quando(a.getDataHora().minus(prazoDeConfirmacao)),
                    semResposta == AcaoSemResposta.LIBERAR
                            ? "Sem resposta, o horário pode ser oferecido a outra pessoa, mas você continua na fila do seu encaminhamento."
                            : "Sem resposta, a equipe da unidade pode entrar em contato com você.");
        };
    }

    public String retornoResposta(RespostaPaciente resposta, Paciente p, Agendamento a) {
        String data = a.getDataHora().format(DATA);
        String hora = a.getDataHora().format(HORA);
        return switch (resposta) {
            case CONFIRMAR -> """
                Obrigado, %s! Sua presença está confirmada para %s, às %s, na %s.
                Na véspera enviaremos um lembrete com as orientações.""".formatted(
                    p.nomeCompleto(), data, hora, a.getUnidade());
            case CANCELAR -> """
                Tudo bem, %s. Cancelamos seu atendimento de %s, às %s, e a vaga será liberada para outra pessoa.
                Se precisar, procure a %s para agendar novamente.""".formatted(
                    p.nomeCompleto(), data, hora, a.getUnidade());
            case REAGENDAR -> """
                Certo, %s! Registramos que você quer outro horário.
                A equipe da %s vai entrar em contato para remarcar seu atendimento.""".formatted(
                    p.nomeCompleto(), a.getUnidade());
        };
    }

    public String naoEntendi(Paciente p, Agendamento a) {
        return """
            Desculpe, %s, não entendi sua resposta sobre o atendimento de %s, às %s, na %s.
            Responda apenas com o número:
            1 - Sim, vou
            2 - Não vou conseguir
            3 - Quero outro horário""".formatted(
                p.nomeCompleto(), a.getDataHora().format(DATA), a.getDataHora().format(HORA), a.getUnidade());
    }

    public String paraCuidador(Paciente p, String texto) {
        return "Olá, %s. Mensagem sobre %s:\n\n%s".formatted(p.nomeCuidador(), p.nomeCompleto(), texto);
    }

    public String quando(Agendamento a) {
        return "%s, às %s".formatted(a.getDataHora().format(DATA), a.getDataHora().format(HORA));
    }

    public String quando(LocalDateTime dataHora) {
        return "%s, às %s".formatted(dataHora.format(DATA), dataHora.format(HORA));
    }

    public String consultaRemarcada(Paciente p, Agendamento anterior, Agendamento nova) {
        return """
            Olá, %s! Sua consulta foi remarcada.
            Antes: %s
            Agora: %s, na %s.
            Endereço: %s
            Envie MENU para ver suas opções.""".formatted(
                primeiroNome(p), quando(anterior), quando(nova), nova.getUnidade(), nova.getEndereco());
    }

    public String consultaCancelada(Paciente p, Agendamento a) {
        return """
            Olá, %s. Sua consulta de %s na %s foi cancelada e a vaga foi liberada.
            Para marcar um novo horário, envie MENU e escolha 1 - Agendar consulta.""".formatted(
                primeiroNome(p), quando(a), a.getUnidade());
    }

    public String portalAtivado(Paciente p) {
        return """
            Olá, %s! Seu acesso ao portal do paciente da Liva foi ativado.
            Se não foi você, procure a recepção da unidade.""".formatted(primeiroNome(p));
    }

    public String menu(Paciente p) {
        return """
            Olá, %s! Sou a assistente da Liva. O que você quer fazer?
            1 - Agendar consulta
            2 - Ver minhas consultas
            3 - Remarcar consulta
            4 - Cancelar consulta
            0 - Sair
            Responda com o número da opção.""".formatted(primeiroNome(p));
    }

    public String escolherPaciente(List<Paciente> pacientes) {
        StringBuilder t = new StringBuilder("Encontramos mais de um cadastro neste número. Sobre quem você quer falar?\n");
        for (int i = 0; i < pacientes.size(); i++)
            t.append(i + 1).append(" - ").append(nome(pacientes.get(i))).append('\n');
        return t.append("Responda com o número.").toString();
    }

    public String lista(String titulo, List<String> itens, String rodape) {
        StringBuilder t = new StringBuilder(titulo).append('\n');
        for (int i = 0; i < itens.size(); i++) t.append(i + 1).append(" - ").append(itens.get(i)).append('\n');
        return rodape == null ? t.toString().trim() : t.append(rodape).toString();
    }

    public String linkDeRemarcacao(Paciente p, Agendamento a, String link, int restantes, long minutos) {
        return """
            %s, para remarcar sua consulta de %s é só tocar no link e escolher o novo horário:
            %s
            O link vale por %d minutos. Seu horário atual continua reservado até você confirmar o novo.
            Você pode remarcar mais %d %s este mês.""".formatted(
                primeiroNome(p), quando(a), link, minutos, restantes, restantes == 1 ? "vez" : "vezes");
    }

    public String linkDeAgendamento(Paciente p, String link, long minutos) {
        return """
            %s, registramos que você quer outro horário. Escolha o novo dia pelo link:
            %s
            O link vale por %d minutos. Se preferir, envie MENU e escolha 1 - Agendar consulta.""".formatted(
                primeiroNome(p), link, minutos);
    }

    public String limiteDeRemarcacoes(Paciente p, int limite) {
        return """
            %s, você já remarcou %d vezes este mês, que é o limite pelo WhatsApp e pelo portal.
            Se precisar mudar de novo, procure a recepção da unidade.""".formatted(primeiroNome(p), limite);
    }

    public String confirmarCancelamento(String descricao) {
        return """
            Você quer mesmo cancelar a consulta de %s?
            1 - Sim, cancelar
            2 - Não, manter""".formatted(descricao);
    }

    public String naoEntendiOpcao() {
        return "Não entendi. Responda com o número de uma das opções, ou envie MENU para recomeçar.";
    }

    public String semCadastro() {
        return "Olá! Não encontramos um cadastro com este número. Procure a recepção da unidade para atualizar seu telefone.";
    }

    public String despedida() {
        return "Tudo certo! Quando precisar, é só mandar uma mensagem.";
    }

    public String paraPessoaDeConfianca(String nomeDoCuidador, Paciente p, String texto) {
        return "Olá, %s. Mensagem sobre %s, de quem você é pessoa de confiança:\n\n%s".formatted(
                primeiroNome(nomeDoCuidador), nome(p), texto);
    }

    public String boasVindasAoCuidador(String nomeDoCuidador, Paciente p, String urlDoPortal) {
        return """
            Olá, %s! Agora você é pessoa de confiança de %s na Liva.
            Você passa a receber os avisos, lembretes e pedidos de confirmação das consultas e pode agendar, remarcar e cancelar por %s.
            Pelo WhatsApp: envie MENU a qualquer momento.
            Pelo portal: %s/cuidador/ativar (use o seu CPF e o CPF de %s).""".formatted(
                primeiroNome(nomeDoCuidador), nome(p), primeiroNome(p), urlDoPortal, primeiroNome(p));
    }

    public String menuParaCuidador(String nomeDoCuidador, Paciente p) {
        return """
            Olá, %s! Você está cuidando de %s. O que você quer fazer?
            1 - Agendar consulta
            2 - Ver as consultas
            3 - Remarcar consulta
            4 - Cancelar consulta
            0 - Sair
            Responda com o número da opção.""".formatted(primeiroNome(nomeDoCuidador), nome(p));
    }

    public String ofertaDeVaga(Paciente p, String especialidade, String profissional, String unidade, String endereco,
                               LocalDateTime inicio, LocalDateTime respondaAte, LocalDateTime antecipaDe, String link) {
        String antes = antecipaDe == null ? "" : "\nSua consulta atual é em %s. Se aceitar, ela é trocada por este novo horário.".formatted(quando(antecipaDe));
        return """
            Olá, %s! Uma vaga de %s ficou disponível para você.
            Quando: %s
            Com: %s
            Onde: %s, %s%s
            Responda ACEITO para ficar com a vaga ou NÃO POSSO se este horário não der.
            Você também pode responder pelo portal: %s
            A oferta vale até %s. Se não puder, você continua na fila do seu encaminhamento na mesma posição.""".formatted(
                primeiroNome(p), especialidade, quando(inicio), profissional, unidade, endereco, antes, link, quando(respondaAte));
    }

    public String vagaConfirmada(Paciente p, Agendamento nova, LocalDateTime anterior) {
        String troca = anterior == null ? "" : "\nA consulta de %s foi liberada para outra pessoa.".formatted(quando(anterior));
        return """
            Pronto, %s! A vaga é sua e a sua presença já está confirmada.
            Quando: %s
            Onde: %s, %s%s
            Leve seu cartão do SUS e um documento com foto.""".formatted(
                primeiroNome(p), quando(nova), nova.getUnidade(), nova.getEndereco(), troca);
    }

    public String ofertaIndisponivel(Paciente p) {
        return """
            %s, esta vaga não está mais disponível. Você continua na fila do seu encaminhamento, na mesma posição.
            Avisaremos por aqui quando surgir outro horário.""".formatted(primeiroNome(p));
    }

    public String ofertaRecusada(Paciente p) {
        return """
            Tudo bem, %s. Registramos que este horário não dá para você.
            Você continua na fila do seu encaminhamento, na mesma posição.""".formatted(primeiroNome(p));
    }

    public String consultaLiberadaSemConfirmacao(Paciente p, Agendamento a) {
        return """
            Olá, %s. Como não recebemos a sua confirmação, o horário de %s na %s foi liberado para outra pessoa.
            Seu encaminhamento continua válido e você segue na fila, sem perder a sua posição.
            Para marcar de novo, envie MENU ou procure a recepção da unidade.""".formatted(
                primeiroNome(p), quando(a), a.getUnidade());
    }

    public String comoChegar(Agendamento a) {
        return MAPA + codificar(a.getEndereco() + ", " + a.getUnidade());
    }

    public String salvarNaAgenda(Agendamento a) {
        LocalDateTime fim = a.getDataHora().plusMinutes(a.getDuracaoMinutos() > 0 ? a.getDuracaoMinutos() : DURACAO_PADRAO);
        return AGENDA
                + "&text=" + codificar("Consulta na " + a.getUnidade())
                + "&dates=" + a.getDataHora().format(AGENDA_HORARIO) + "/" + fim.format(AGENDA_HORARIO)
                + "&location=" + codificar(a.getEndereco() + ", " + a.getUnidade())
                + "&details=" + codificar("Leve seu cartão do SUS e um documento com foto.");
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    public String perguntaAposFalta(Paciente p, Agendamento a, String link, long minutos) {
        String remarcar = link == null ? "Para marcar de novo, envie MENU ou procure a recepção da unidade."
                : "Para marcar um novo horário, toque no link (vale por %d minutos): %s. Depois disso, envie MENU.".formatted(minutos, link);
        return """
            Olá, %s. Sentimos sua falta no atendimento de %s na %s.
            %s
            Pode contar o que aconteceu? Isso ajuda a unidade a melhorar.
            %s
            Responda com o número.""".formatted(primeiroNome(p), quando(a), a.getUnidade(), remarcar, opcoesDeMotivo());
    }

    public String perguntaAposCancelamento(Paciente p) {
        return """
            %s, para a unidade se organizar melhor, pode contar por que não vai conseguir ir?
            %s
            Responda com o número. Se preferir não responder, tudo bem.""".formatted(primeiroNome(p), opcoesDeMotivo());
    }

    public String naoEntendiMotivo() {
        return """
            Não entendi. Responda só com o número do motivo:
            %s
            Se quiser outra coisa, envie MENU.""".formatted(opcoesDeMotivo());
    }

    public String motivoRegistrado(Paciente p, MotivoDeAusencia motivo) {
        String complemento = switch (motivo) {
            case ESQUECI -> "Vamos reforçar os lembretes das suas próximas consultas.";
            case TRANSPORTE -> "Se precisar, a recepção pode ajudar a encontrar um horário ou unidade mais fácil para você.";
            case NAO_PRECISA -> "Se voltar a precisar, procure a unidade ou envie MENU.";
            default -> "Quando quiser marcar de novo, envie MENU.";
        };
        return "Obrigado, %s. Anotamos: %s. %s".formatted(primeiroNome(p), motivo.getRotulo().toLowerCase(PT_BR), complemento);
    }

    private static String opcoesDeMotivo() {
        StringBuilder t = new StringBuilder();
        for (MotivoDeAusencia m : MotivoDeAusencia.values()) {
            if (!t.isEmpty()) t.append('\n');
            t.append(m.getNumero()).append(" - ").append(m.getRotulo());
        }
        return t.toString();
    }

    private static String primeiroNome(String nome) {
        return nome.trim().split("\\s+")[0];
    }

    private static String nome(Paciente p) {
        return p.nomeSocial() != null && !p.nomeSocial().isBlank() ? p.nomeSocial() : p.nomeCompleto();
    }

    private static String primeiroNome(Paciente p) {
        return nome(p).trim().split("\\s+")[0];
    }
}