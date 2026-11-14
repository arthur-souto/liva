package sus.liva.domain.model.ausencia;

import java.util.ArrayList;
import java.util.List;

public record RiscoDeFalta(int pontos, NivelDeRisco nivel, List<String> fatores) {
    private static final int ALTO = 50;
    private static final int MEDIO = 25;

    public static RiscoDeFalta avaliar(SinaisDeRisco s) {
        int pontos = 0;
        List<String> fatores = new ArrayList<>();
        if (s.faltasRecentes() > 0) {
            pontos += Math.min(2, s.faltasRecentes()) * 25;
            fatores.add(s.faltasRecentes() == 1 ? "Faltou 1 vez nos últimos 12 meses" : "Faltou " + s.faltasRecentes() + " vezes nos últimos 12 meses");
        }
        if (s.mensagemNaoEntregue()) {
            pontos += 25;
            fatores.add("O WhatsApp não recebeu as mensagens");
        }
        if (s.pedidoSemResposta()) {
            pontos += 20;
            fatores.add("Não respondeu ao pedido de confirmação");
        }
        if (s.diasDeAntecedencia() != null && s.diasDeAntecedencia() > 30) {
            pontos += 15;
            fatores.add("Marcada há mais de 30 dias");
        }
        if (s.cancelamentosRecentes() >= 2) {
            pontos += 10;
            fatores.add("Cancelou ou remarcou " + s.cancelamentosRecentes() + " vezes em 90 dias");
        }
        if (s.naoAtendeuLigacao()) {
            pontos += 10;
            fatores.add("Não atendeu a ligação da unidade");
        }
        if (s.primeiraConsulta()) {
            pontos += 5;
            fatores.add("Primeira consulta na unidade");
        }
        if (s.comparecimentosRecentes() >= 3 && s.faltasRecentes() == 0) {
            pontos -= 10;
            fatores.add("Compareceu a todas as últimas consultas");
        }
        if (s.confirmada()) {
            pontos -= 40;
            fatores.add("Confirmou presença");
        }
        int limitado = Math.max(0, Math.min(100, pontos));
        NivelDeRisco nivel = limitado >= ALTO ? NivelDeRisco.ALTO : limitado >= MEDIO ? NivelDeRisco.MEDIO : NivelDeRisco.BAIXO;
        return new RiscoDeFalta(limitado, nivel, List.copyOf(fatores));
    }
}
