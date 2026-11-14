package sus.liva.application.usecase;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.paciente.Paciente;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ModeloMensagens {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM (EEEE)", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public String montar(TipoLembrete tipo, Paciente p, Agendamento a) {
        String data = a.getDataHora().format(DATA);
        String hora = a.getDataHora().format(HORA);
        return switch (tipo) {
            case CONFIRMACAO_AGENDAMENTO -> """
                Olá, %s! Seu atendimento na %s foi agendado para %s, às %s.
                Endereço: %s
                Leve seu cartão do SUS e um documento com foto.
                Vamos te lembrar alguns dias antes.""".formatted(
                    p.nomeCompleto(), a.getUnidade(), data, hora, a.getEndereco());
            case PEDIDO_CONFIRMACAO -> """
                Olá, %s! Você tem atendimento na %s em %s, às %s.
                Você consegue ir?
                1 - Sim, vou
                2 - Não vou conseguir
                3 - Quero outro horário
                Responda com o número ou por escrito.""".formatted(
                    p.nomeCompleto(), a.getUnidade(), data, hora);
            case LEMBRETE_VESPERA -> {
                String t = "Lembrete: amanhã, %s, às %s, você tem atendimento na %s.\nEndereço: %s"
                        .formatted(data, hora, a.getUnidade(), a.getEndereco());
                if (a.getPreparo() != null && !a.getPreparo().isBlank()) t += "\nPreparo: " + a.getPreparo();
                yield t + "\nSe não puder ir, responda 2 para liberar a vaga para outra pessoa.";
            }
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