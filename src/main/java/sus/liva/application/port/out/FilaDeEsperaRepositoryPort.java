package sus.liva.application.port.out;

import sus.liva.application.port.in.ConsultaSemConfirmacao;
import sus.liva.application.port.in.FiltroDaFila;
import sus.liva.application.port.in.IndicadoresDaFila;
import sus.liva.application.port.in.MovimentacaoDaFila;
import sus.liva.application.port.in.OfertaNoPainel;
import sus.liva.application.port.in.OfertaParaPaciente;
import sus.liva.application.port.in.PacienteNaFila;
import sus.liva.application.port.in.Pagina;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.PosicaoNaFila;
import sus.liva.application.port.in.VagaNoPainel;
import sus.liva.domain.model.fila.DecisaoSemResposta;
import sus.liva.domain.model.fila.EntradaNaFila;
import sus.liva.domain.model.fila.OfertaDeVaga;
import sus.liva.domain.model.fila.VagaLiberada;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FilaDeEsperaRepositoryPort {
    List<EntradaNaFila> incluirEntradasPendentes(UUID pacienteId, Instant agora);
    List<EntradaNaFila> encerrarEntradasSemNecessidade(UUID pacienteId, Instant agora);
    List<EntradaNaFila> reativarEntradas(UUID pacienteId, Instant agora);
    List<VagaLiberada> registrarVagasLiberadas(LocalDateTime aPartirDe, Instant agora);

    Optional<EntradaNaFila> buscarEntrada(UUID id);
    Optional<EntradaNaFila> entradaParaAtualizar(UUID id);
    Optional<VagaLiberada> buscarVaga(UUID id);
    Optional<VagaLiberada> vagaParaAtualizar(UUID id);
    Optional<VagaLiberada> proximaVagaParaOfertar(Instant agora);
    Optional<OfertaDeVaga> buscarOferta(UUID id);
    Optional<OfertaDeVaga> ofertaParaAtualizar(UUID id);
    List<OfertaDeVaga> ofertasVencidasParaAtualizar(Instant agora, int limite);
    List<OfertaDeVaga> ofertasSemEnvio(Instant agora, Instant criadasAte, Instant tentadasAte, int limite);
    List<OfertaDeVaga> ofertasAtivasDosPacientes(Collection<UUID> pacientes);
    Optional<OfertaDeVaga> ultimaOfertaDosPacientes(Collection<UUID> pacientes, Instant desde);

    Optional<EntradaNaFila> proximaElegivel(VagaLiberada vaga);
    Optional<UUID> consultaQueOcupaAgenda(UUID encaminhamentoId);
    boolean horarioOcupado(UUID profissionalId, LocalDateTime inicio, LocalDateTime fim);
    boolean pacienteOcupado(UUID pacienteId, LocalDateTime inicio, LocalDateTime fim, UUID excetoConsulta);
    boolean horarioEmOfertaParaOutro(UUID profissionalId, LocalDateTime inicio, LocalDateTime fim, UUID pacienteId);
    List<VagaLiberada> vagasEmOfertaParaOutros(UUID profissionalId, LocalDateTime de, LocalDateTime ate, UUID pacienteId);
    int ofertasDaVaga(UUID vagaId);

    void salvarEntrada(EntradaNaFila entrada);
    void salvarVaga(VagaLiberada vaga);
    void salvarOferta(OfertaDeVaga oferta);
    boolean reivindicarEnvio(UUID ofertaId, Instant agora, Instant tentadasAte);
    boolean reivindicar(String chave, Instant agora);
    void registrar(Movimentacao movimentacao);

    List<UUID> consultasParaAvisoFinal(LocalDateTime de, LocalDateTime ate, int limite);
    List<UUID> consultasComPrazoVencido(LocalDateTime agora, LocalDateTime limite, int quantidade);
    boolean pedidoDeConfirmacaoEntregue(UUID agendamentoId, boolean exigirEntrega);
    boolean registrarDecisao(UUID agendamentoId, DecisaoSemResposta decisao, Instant em, UUID atorId, String papel);
    void substituirDecisao(UUID agendamentoId, DecisaoSemResposta decisao, Instant em, UUID atorId, String papel);

    List<PosicaoNaFila> posicoesDoPaciente(UUID pacienteId, Instant recentesDesde);
    List<OfertaParaPaciente> ofertasAtivasDoPaciente(UUID pacienteId);
    Optional<PosicaoNaFila> posicao(UUID entradaId);
    Pagina<PacienteNaFila> fila(FiltroDaFila filtro, PedidoDePagina pagina);
    IndicadoresDaFila indicadores(FiltroDaFila filtro, Instant desde, LocalDateTime desdeLocal, LocalDateTime agora);
    List<VagaNoPainel> vagas(FiltroDaFila filtro, int limite);
    List<OfertaNoPainel> ofertas(FiltroDaFila filtro, UUID entradaId, int limite);
    List<ConsultaSemConfirmacao> semConfirmacao(FiltroDaFila filtro, LocalDateTime agora, int limite);
    List<MovimentacaoDaFila> movimentacoes(FiltroDaFila filtro, UUID entradaId, int limite);
}
