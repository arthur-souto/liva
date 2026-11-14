package sus.liva.application.port.out;

import sus.liva.application.port.in.Ator;

import java.time.Duration;

public interface EmissorDeTokenPort {
    TokenEmitido emitir(Ator ator, String nome, Duration validade);
}
