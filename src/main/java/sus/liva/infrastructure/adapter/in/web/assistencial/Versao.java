package sus.liva.infrastructure.adapter.in.web.assistencial;

final class Versao {
    private Versao() {
    }

    static long exigir(Long versao) {
        if (versao == null) throw new IllegalArgumentException("Informe a versão do registro (campo versao) que você está alterando");
        return versao;
    }
}
