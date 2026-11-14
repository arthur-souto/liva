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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.CadastroDePacienteUseCase;
import sus.liva.application.port.in.FiltroDeAtendimentos;
import sus.liva.application.port.in.HistoricoDoPacienteUseCase;
import sus.liva.application.port.in.PedidoDePagina;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Pacientes", description = "Cadastro na recepção e histórico por triagem")
public class PacienteController {
    private final CadastroDePacienteUseCase cadastro;
    private final HistoricoDoPacienteUseCase historico;

    public PacienteController(CadastroDePacienteUseCase cadastro, HistoricoDoPacienteUseCase historico) {
        this.cadastro = cadastro;
        this.historico = historico;
    }

    @PostMapping
    @Operation(summary = "Cadastra um paciente", description = "Recusa CPF ou Cartão SUS já cadastrados (409).")
    public ResponseEntity<PacienteResponse> cadastrar(Ator ator, @RequestBody PacienteRequest request) {
        PacienteResponse criado = PacienteResponse.de(cadastro.cadastrar(ator, request.paraDados()));
        return ResponseEntity.created(URI.create("/api/pacientes/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Pesquisa pacientes por nome (ao menos 3 letras), CPF ou Cartão SUS", description = "Sem termo, devolve os últimos 50 cadastrados.")
    public List<PacienteResponse> pesquisar(Ator ator, @RequestParam(defaultValue = "") String termo) {
        return cadastro.pesquisar(ator, termo).stream().map(PacienteResponse::de).toList();
    }

    @GetMapping("/pagina")
    @Operation(summary = "Pesquisa paginada de pacientes por nome (ao menos 3 letras), CPF ou Cartão SUS",
            description = "Sem termo, lista os cadastrados mais recentes. Tamanho máximo da página: 50.")
    public PacientesResponse pesquisarPaginado(Ator ator, @RequestParam(defaultValue = "") String termo,
                                               @RequestParam(required = false) Integer pagina,
                                               @RequestParam(required = false) Integer tamanho) {
        return PacientesResponse.de(cadastro.pesquisar(ator, termo, PedidoDePagina.de(pagina, tamanho)));
    }

    @GetMapping("/{id}")
    public PacienteResponse buscar(Ator ator, @PathVariable UUID id) {
        return PacienteResponse.de(cadastro.buscar(ator, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o cadastro", description = "Envie a versao recebida; se outra pessoa alterou antes, responde 409.")
    public PacienteResponse atualizar(Ator ator, @PathVariable UUID id, @RequestBody PacienteRequest request) {
        return PacienteResponse.de(cadastro.atualizar(ator, id, request.paraDados(), Versao.exigir(request.versao())));
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico do paciente, um ramo por triagem",
            description = "Os dados clínicos de cada ramo só aparecem para quem participa do atendimento.")
    public HistoricoResponse historico(Ator ator, @PathVariable UUID id) {
        return HistoricoResponse.de(historico.historico(ator, id));
    }

    @GetMapping("/{id}/atendimentos")
    @Operation(summary = "Atendimentos do paciente, paginados",
            description = "Filtros opcionais: especialidade (código), andamento (ABERTO ou ENCERRADO) e ano. Tamanho máximo da página: 50.")
    public AtendimentosResponse atendimentos(Ator ator, @PathVariable UUID id, @RequestParam(required = false) String especialidade,
                                             @RequestParam(required = false) String andamento, @RequestParam(required = false) Integer ano,
                                             @RequestParam(required = false) Integer pagina, @RequestParam(required = false) Integer tamanho) {
        return AtendimentosResponse.de(historico.atendimentos(ator, id,
                new FiltroDeAtendimentos(especialidade, Requisicoes.andamento(andamento), ano), PedidoDePagina.de(pagina, tamanho)));
    }
}
