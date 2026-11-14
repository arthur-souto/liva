package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;

import java.util.UUID;

public interface FilaDeEsperaUseCase {
    MinhaFila minhaFila(Ator ator);
    Agendamento aceitar(Ator ator, UUID ofertaId);
    void recusar(Ator ator, UUID ofertaId);
    PainelDaFila painel(Ator ator, FiltroDaFila filtro, PedidoDePagina pagina);
    DetalheDaEntrada detalhar(Ator ator, UUID entradaId);
    void pausar(Ator ator, UUID entradaId, String motivo);
    void retomar(Ator ator, UUID entradaId);
    Agendamento confirmarPresenca(Ator ator, UUID consultaId);
    ResumoDoCiclo processarAgora(Ator ator);
}
