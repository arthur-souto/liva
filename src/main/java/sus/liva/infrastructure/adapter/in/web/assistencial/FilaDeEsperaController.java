package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.ConsultaSemConfirmacao;
import sus.liva.application.port.in.DetalheDaEntrada;
import sus.liva.application.port.in.FilaDeEsperaUseCase;
import sus.liva.application.port.in.FiltroDaFila;
import sus.liva.application.port.in.IndicadoresDaFila;
import sus.liva.application.port.in.MinhaFila;
import sus.liva.application.port.in.MovimentacaoDaFila;
import sus.liva.application.port.in.OfertaNoPainel;
import sus.liva.application.port.in.OfertaParaPaciente;
import sus.liva.application.port.in.PacienteNaFila;
import sus.liva.application.port.in.PainelDaFila;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.PosicaoNaFila;
import sus.liva.application.port.in.ResumoDoCiclo;
import sus.liva.application.port.in.VagaNoPainel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Fila de espera", description = "Fila por encaminhamento, ofertas de vagas liberadas e confirmação de presença")
public class FilaDeEsperaController {
    private final FilaDeEsperaUseCase fila;

    public FilaDeEsperaController(FilaDeEsperaUseCase fila) {
        this.fila = fila;
    }

    public record MinhaFilaResponse(Instant agora, List<PosicaoNaFila> filas, List<OfertaParaPaciente> ofertas) {
        static MinhaFilaResponse de(MinhaFila f) {
            return new MinhaFilaResponse(f.agora(), f.filas(), f.ofertas());
        }
    }

    public record IndicadoresResponse(long aguardando, long pausadas, long vagasAbertas, long ofertasAtivas, long aguardandoConfirmacao,
                                      long semResposta, long vagasReaproveitadas, long ofertasRecusadas, long ofertasExpiradas,
                                      Double taxaDeConfirmacao, Double taxaDeAbsenteismo, Instant desde) {
        static IndicadoresResponse de(IndicadoresDaFila i) {
            return new IndicadoresResponse(i.aguardando(), i.pausadas(), i.vagasAbertas(), i.ofertasAtivas(), i.aguardandoConfirmacao(),
                    i.semResposta(), i.vagasReaproveitadas(), i.ofertasRecusadas(), i.ofertasExpiradas(), i.taxaDeConfirmacao(),
                    i.taxaDeAbsenteismo(), i.desde());
        }
    }

    public record PainelResponse(Instant agora, FiltroDaFila filtro, boolean podeIntervir, IndicadoresResponse indicadores,
                                 List<PacienteNaFila> itens, int pagina, int tamanho, long total, int totalPaginas,
                                 List<VagaNoPainel> vagas, List<OfertaNoPainel> ofertas, List<ConsultaSemConfirmacao> semConfirmacao,
                                 List<MovimentacaoDaFila> movimentacoes) {
        static PainelResponse de(PainelDaFila p) {
            return new PainelResponse(p.agora(), p.filtro(), p.podeIntervir(), IndicadoresResponse.de(p.indicadores()), p.fila().itens(),
                    p.fila().numero(), p.fila().tamanho(), p.fila().total(), p.fila().totalPaginas(), p.vagas(), p.ofertas(),
                    p.semConfirmacao(), p.movimentacoes());
        }
    }

    public record PausaRequest(@NotBlank(message = "Informe o motivo da pausa") @Size(max = 500) String motivo) {}

    @GetMapping("/api/portal/fila")
    @Operation(summary = "Situação do paciente logado nas filas de cada encaminhamento e as vagas oferecidas a ele")
    public MinhaFilaResponse minhaFila(Ator ator) {
        return MinhaFilaResponse.de(fila.minhaFila(ator));
    }

    @PostMapping("/api/portal/fila/ofertas/{ofertaId}/aceitar")
    @Operation(summary = "Aceita uma vaga oferecida", description = "A reserva é atômica. 409 se a vaga não estiver mais disponível ou o prazo tiver acabado.")
    public ConsultaResponse aceitar(Ator ator, @PathVariable UUID ofertaId) {
        return ConsultaResponse.de(fila.aceitar(ator, ofertaId));
    }

    @PostMapping("/api/portal/fila/ofertas/{ofertaId}/recusar")
    @Operation(summary = "Recusa uma vaga oferecida sem perder a posição na fila")
    public ResponseEntity<Void> recusar(Ator ator, @PathVariable UUID ofertaId) {
        fila.recusar(ator, ofertaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/filas")
    @Operation(summary = "Painel das filas de espera", description = "Especialistas veem apenas a própria especialidade e agenda.")
    public PainelResponse painel(Ator ator, @RequestParam(required = false) String especialidade,
                                 @RequestParam(required = false) UUID profissional, @RequestParam(required = false) String unidade,
                                 @RequestParam(required = false) Integer pagina, @RequestParam(required = false) Integer tamanho) {
        return PainelResponse.de(fila.painel(ator, new FiltroDaFila(especialidade, profissional, unidade), PedidoDePagina.de(pagina, tamanho)));
    }

    @GetMapping("/api/filas/entradas/{entradaId}")
    @Operation(summary = "Detalhe de uma entrada na fila: posição, ofertas e movimentações")
    public DetalheDaEntrada detalhar(Ator ator, @PathVariable UUID entradaId) {
        return fila.detalhar(ator, entradaId);
    }

    @PostMapping("/api/filas/entradas/{entradaId}/pausar")
    @Operation(summary = "Pausa uma entrada na fila com justificativa auditada")
    public ResponseEntity<Void> pausar(Ator ator, @PathVariable UUID entradaId, @Valid @RequestBody PausaRequest corpo) {
        fila.pausar(ator, entradaId, corpo.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/filas/entradas/{entradaId}/retomar")
    @Operation(summary = "Retoma uma entrada pausada mantendo a data de entrada original")
    public ResponseEntity<Void> retomar(Ator ator, @PathVariable UUID entradaId) {
        fila.retomar(ator, entradaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/filas/ofertas/{ofertaId}/aceitar")
    @Operation(summary = "A equipe registra o aceite que a pessoa deu por telefone ou na unidade")
    public ConsultaResponse aceitarPelaEquipe(Ator ator, @PathVariable UUID ofertaId) {
        return ConsultaResponse.de(fila.aceitar(ator, ofertaId));
    }

    @PostMapping("/api/filas/ofertas/{ofertaId}/recusar")
    @Operation(summary = "A equipe registra a recusa que a pessoa deu por telefone ou na unidade")
    public ResponseEntity<Void> recusarPelaEquipe(Ator ator, @PathVariable UUID ofertaId) {
        fila.recusar(ator, ofertaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/filas/consultas/{consultaId}/confirmar-presenca")
    @Operation(summary = "Confirmação de presença assistida pela recepção")
    public ConsultaResponse confirmarPresenca(Ator ator, @PathVariable UUID consultaId) {
        return ConsultaResponse.de(fila.confirmarPresenca(ator, consultaId));
    }

    @PostMapping("/api/filas/processar")
    @Operation(summary = "Executa agora o ciclo automático da fila (o mesmo do agendador)")
    public ResumoDoCiclo processar(Ator ator) {
        return fila.processarAgora(ator);
    }
}
