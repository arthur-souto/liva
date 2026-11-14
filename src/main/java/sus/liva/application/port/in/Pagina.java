package sus.liva.application.port.in;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> itens, int numero, int tamanho, long total) {

    public int totalPaginas() {
        return (int) Math.max(1, (total + tamanho - 1) / tamanho);
    }

    public <R> Pagina<R> map(Function<T, R> conversor) {
        return new Pagina<>(itens.stream().map(conversor).toList(), numero, tamanho, total);
    }

    public static <T> Pagina<T> de(List<T> todos, PedidoDePagina pedido) {
        int ultima = (int) Math.max(1, (todos.size() + pedido.tamanho() - 1L) / pedido.tamanho());
        int numero = Math.min(pedido.numero(), ultima);
        int inicio = Math.min((numero - 1) * pedido.tamanho(), todos.size());
        int fim = Math.min(inicio + pedido.tamanho(), todos.size());

        return new Pagina<>(List.copyOf(todos.subList(inicio, fim)), numero, pedido.tamanho(), todos.size());
    }
}
