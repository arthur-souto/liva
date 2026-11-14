package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.TransacaoPort;

import java.util.function.Supplier;

public class TransacaoFake implements TransacaoPort {
    private boolean ativa;
    private int execucoes;

    @Override
    public <T> T executar(Supplier<T> operacao) {
        ativa = true;
        execucoes++;

        try {
            return operacao.get();
        } finally {
            ativa = false;
        }
    }

    public boolean ativa() {
        return ativa;
    }

    public int execucoes() {
        return execucoes;
    }
}
