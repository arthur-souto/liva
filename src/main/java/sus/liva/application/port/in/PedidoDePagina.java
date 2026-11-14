package sus.liva.application.port.in;

public record PedidoDePagina(int numero, int tamanho) {
    public static final int TAMANHO_PADRAO = 10;
    public static final int TAMANHO_MAXIMO = 50;

    public static PedidoDePagina de(Integer numero, Integer tamanho) {
        int n = numero == null || numero < 1 ? 1 : numero;
        int t = tamanho == null || tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
        return new PedidoDePagina(n, t);
    }

    public int deslocamento() {
        return (numero - 1) * tamanho;
    }
}
