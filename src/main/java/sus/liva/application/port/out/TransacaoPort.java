package sus.liva.application.port.out;

import java.util.function.Supplier;

public interface TransacaoPort {
    <T> T executar(Supplier<T> operacao);
}
