package sus.liva.application.usecase;

import sus.liva.application.port.in.Ator;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.Movimentacao;
import sus.liva.domain.model.fila.EntradaNaFila;
import sus.liva.domain.model.fila.EventoDaFila;
import sus.liva.domain.model.fila.OfertaDeVaga;
import sus.liva.domain.model.fila.VagaLiberada;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

final class DiarioDaFila {
    private static final String SISTEMA = "SISTEMA";

    private final FilaDeEsperaRepositoryPort fila;
    private final Clock clock;

    DiarioDaFila(FilaDeEsperaRepositoryPort fila, Clock clock) {
        this.fila = fila;
        this.clock = clock;
    }

    void daEntrada(EventoDaFila evento, EntradaNaFila e, String detalhe, Ator ator) {
        registrar(evento, e.getId(), null, null, null, e.getPacienteId(), detalhe, ator);
    }

    void daVaga(EventoDaFila evento, VagaLiberada v, String detalhe) {
        registrar(evento, null, null, v.getId(), null, null, detalhe, null);
    }

    void daOferta(EventoDaFila evento, OfertaDeVaga o, UUID agendamentoId, String detalhe, Ator ator) {
        registrar(evento, o.getEntradaId(), o.getId(), o.getVagaId(), agendamentoId, o.getPacienteId(), detalhe, ator);
    }

    void daConsulta(EventoDaFila evento, UUID agendamentoId, UUID pacienteId, String detalhe, Ator ator) {
        registrar(evento, null, null, null, agendamentoId, pacienteId, detalhe, ator);
    }

    private void registrar(EventoDaFila evento, UUID entrada, UUID oferta, UUID vaga, UUID agendamento, UUID paciente, String detalhe,
                           Ator ator) {
        fila.registrar(new Movimentacao(entrada, oferta, vaga, agendamento, paciente, evento, detalhe,
                ator == null ? null : ator.usuarioId(), ator == null ? SISTEMA : ator.papel().name(), Instant.now(clock)));
    }
}
