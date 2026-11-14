package sus.liva.domain.model.triagem;

public record AvaliacaoClinica(
        String queixaPrincipal,
        String sintomas,
        String historicoClinico,
        String antecedentes,
        String observacoes
) {
    public static AvaliacaoClinica vazia() {
        return new AvaliacaoClinica(null, null, null, null, null);
    }
}
