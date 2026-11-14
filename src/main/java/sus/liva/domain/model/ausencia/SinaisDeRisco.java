package sus.liva.domain.model.ausencia;

public record SinaisDeRisco(
        int faltasRecentes,
        int comparecimentosRecentes,
        int cancelamentosRecentes,
        boolean confirmada,
        boolean pedidoSemResposta,
        boolean mensagemNaoEntregue,
        Integer diasDeAntecedencia,
        boolean primeiraConsulta,
        boolean naoAtendeuLigacao
) {}
