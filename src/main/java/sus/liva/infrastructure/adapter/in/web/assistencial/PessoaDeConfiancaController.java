package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.NovaPessoaDeConfianca;
import sus.liva.application.port.in.PessoaDeConfiancaUseCase;
import sus.liva.domain.model.cuidador.CuidadorVinculado;
import sus.liva.domain.model.cuidador.Parentesco;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pacientes/{pacienteId}/pessoas-de-confianca")
@Tag(name = "Pessoas de confiança", description = "Familiares ou cuidadores autorizados a agendar e receber os avisos do paciente")
public class PessoaDeConfiancaController {
    private final PessoaDeConfiancaUseCase pessoasDeConfianca;

    public PessoaDeConfiancaController(PessoaDeConfiancaUseCase pessoasDeConfianca) {
        this.pessoasDeConfianca = pessoasDeConfianca;
    }

    public record PessoaDeConfiancaRequest(String nome, String cpf, String telefone, Parentesco parentesco, boolean autorizacaoRegistrada) {}

    public record PessoaDeConfiancaResponse(UUID id, String nome, String cpf, String telefone, Parentesco parentesco, Instant autorizadoEm) {
        static PessoaDeConfiancaResponse de(CuidadorVinculado v) {
            return new PessoaDeConfiancaResponse(v.cuidador().id(), v.cuidador().nome(), v.cuidador().cpf(),
                    PacienteResponse.digitos(v.cuidador().telefone()), v.parentesco(), v.autorizadoEm());
        }
    }

    @GetMapping
    @Operation(summary = "Lista as pessoas de confiança ativas do paciente")
    public List<PessoaDeConfiancaResponse> listar(Ator ator, @PathVariable UUID pacienteId) {
        return pessoasDeConfianca.doPaciente(ator, pacienteId).stream().map(PessoaDeConfiancaResponse::de).toList();
    }

    @PostMapping
    @Operation(summary = "Registra uma pessoa de confiança",
            description = "Exige a autorização do paciente ou do responsável legal. A pessoa recebe as instruções de acesso pelo WhatsApp.")
    public ResponseEntity<PessoaDeConfiancaResponse> vincular(Ator ator, @PathVariable UUID pacienteId,
                                                              @RequestBody PessoaDeConfiancaRequest r) {
        CuidadorVinculado vinculado = pessoasDeConfianca.vincular(ator, pacienteId,
                new NovaPessoaDeConfianca(r.nome(), r.cpf(), r.telefone(), r.parentesco(), r.autorizacaoRegistrada()));
        return ResponseEntity.status(201).body(PessoaDeConfiancaResponse.de(vinculado));
    }

    @DeleteMapping("/{cuidadorId}")
    @Operation(summary = "Revoga o acesso de uma pessoa de confiança")
    public ResponseEntity<Void> revogar(Ator ator, @PathVariable UUID pacienteId, @PathVariable UUID cuidadorId) {
        pessoasDeConfianca.revogar(ator, pacienteId, cuidadorId);
        return ResponseEntity.noContent().build();
    }
}
