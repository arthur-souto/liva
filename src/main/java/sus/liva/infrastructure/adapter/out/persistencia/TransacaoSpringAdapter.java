package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import sus.liva.application.port.out.TransacaoPort;

import java.util.function.Supplier;

@Component
public class TransacaoSpringAdapter implements TransacaoPort {
    private final TransactionTemplate transactionTemplate;

    TransacaoSpringAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T executar(Supplier<T> operacao) {
        return transactionTemplate.execute(status -> operacao.get());
    }
}
