package sus.liva.application.port.in;

import java.util.UUID;

public interface HistoricoDoPacienteUseCase {
    HistoricoDoPaciente historico(Ator ator, UUID pacienteId);
    AtendimentosDoPaciente atendimentos(Ator ator, UUID pacienteId, FiltroDeAtendimentos filtro, PedidoDePagina pedido);
    ConsultasDoPaciente consultas(Ator ator, UUID pacienteId, FiltroDeConsultas filtro, PedidoDePagina pedido);
    boolean podeAcessarPortal(UUID pacienteId);
}
