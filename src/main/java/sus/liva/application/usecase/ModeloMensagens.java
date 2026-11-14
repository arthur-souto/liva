package sus.liva.application.usecase;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.paciente.Paciente;

import java.time.format.DateTimeFormatter;
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
                Pode responder com o número, por escrito ou com um áudio.""".formatted(
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

    public String paraCuidador(Paciente p, String texto) {
        return "Olá, %s. Mensagem sobre %s:\n\n%s".formatted(p.nomeCuidador(), p.nomeCompleto(), texto);
    }
}