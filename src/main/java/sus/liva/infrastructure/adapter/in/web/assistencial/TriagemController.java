package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.ItemDaFila;
import sus.liva.application.port.in.NovoEncaminhamento;
import sus.liva.application.port.in.TriagemUseCase;
import sus.liva.domain.model.triagem.Retificacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/triagens")
@Tag(name = "Triagens", description = "Fila, avaliação do clínico geral e encaminhamentos")
public class TriagemController {
    private final TriagemUseCase triagens;

    public TriagemController(TriagemUseCase triagens) {
        this.triagens = triagens;
    }

    @PostMapping
    @Operation(summary = "Registra a chegada do paciente e o coloca na fila de triagem")
    public ResponseEntity<TriagemResponse> registrarChegada(Ator ator, @RequestBody Requisicoes.Chegada request) {
        if (request.pacienteId() == null) throw new IllegalArgumentException("Informe o paciente");
        TriagemResponse criada = TriagemResponse.de(triagens.registrarChegada(ator, request.pacienteId()));
        return ResponseEntity.created(URI.create("/api/triagens/" + criada.id())).body(criada);
    }

    @GetMapping("/fila")
    public List<ItemDaFila> fila(Ator ator) {
        return triagens.fila(ator);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha a triagem com encaminhamentos, consultas, retificações e evoluções")
    public RamoResponse detalhar(Ator ator, @PathVariable UUID id) {
        return RamoResponse.de(triagens.detalhar(ator, id));
    }

    @PostMapping("/{id}/iniciar")
    public TriagemResponse iniciar(Ator ator, @PathVariable UUID id) {
        return TriagemResponse.de(triagens.iniciar(ator, id));
    }

    @PutMapping("/{id}/avaliacao")
    public TriagemResponse avaliar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.Avaliacao request) {
        return TriagemResponse.de(triagens.registrarAvaliacao(ator, id, request.avaliacao(), request.sinaisVitais(),
                request.prioridade(), request.justificativaPrioridade(), Versao.exigir(request.versao())));
    }

    @PostMapping("/{id}/encaminhamentos")
    @Operation(summary = "Encaminha a um especialista",
            description = "ELETIVO fica pendente até a triagem ser finalizada; IMEDIATO exige prioridade grave e vai ao plantão.")
    public ResponseEntity<EncaminhamentoResponse> encaminhar(Ator ator, @PathVariable UUID id,
                                                             @RequestBody NovoEncaminhamento request) {
        EncaminhamentoResponse criado = EncaminhamentoResponse.de(triagens.encaminhar(ator, id, request));
        return ResponseEntity.created(URI.create("/api/encaminhamentos/" + criado.id())).body(criado);
    }

    @PostMapping("/{id}/finalizar")
    public TriagemResponse finalizar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.ComVersao request) {
        return TriagemResponse.de(triagens.finalizar(ator, id, Versao.exigir(request.versao())));
    }

    @PostMapping("/{id}/cancelar")
    public TriagemResponse cancelar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.Motivo request) {
        return TriagemResponse.de(triagens.cancelar(ator, id, request.motivo()));
    }

    @PostMapping("/{id}/retificacoes")
    public ResponseEntity<Retificacao> retificar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.NovaRetificacao request) {
        return ResponseEntity.status(201).body(triagens.retificar(ator, id, request.texto(), request.justificativa()));
    }

    @PostMapping("/{id}/encerrar")
    public TriagemResponse encerrar(Ator ator, @PathVariable UUID id) {
        return TriagemResponse.de(triagens.encerrar(ator, id));
    }
}
