package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.PainelDeFaltas;
import sus.liva.application.port.in.PrevencaoDeFaltasUseCase;
import sus.liva.domain.model.ausencia.MotivoDeAusencia;
import sus.liva.domain.model.ausencia.ResultadoDaLigacao;

import java.util.UUID;

@RestController
@Tag(name = "Prevenção de faltas", description = "Risco de falta das próximas consultas, motivos das ausências e evolução do comparecimento")
public class PrevencaoDeFaltasController {
    private final PrevencaoDeFaltasUseCase prevencao;

    public PrevencaoDeFaltasController(PrevencaoDeFaltasUseCase prevencao) {
        this.prevencao = prevencao;
    }

    public record MotivoRequest(@NotNull(message = "Informe o motivo") MotivoDeAusencia motivo) {}

    public record LigacaoRequest(@NotNull(message = "Informe o resultado da ligação") ResultadoDaLigacao resultado) {}

    @GetMapping("/api/faltas")
    @Operation(summary = "Painel de prevenção de faltas", description = "Evolução semanal, motivos dos últimos 90 dias e consultas dos próximos 7 dias por risco.")
    public PainelDeFaltas painel(Ator ator, @RequestParam(defaultValue = "12") int semanas) {
        return prevencao.painel(ator, semanas);
    }

    @PostMapping("/api/consultas/{id}/motivo-de-ausencia")
    @Operation(summary = "Registra por que o paciente faltou ou cancelou", description = "Disponível para consultas canceladas ou com falta.")
    public ResponseEntity<Void> registrarMotivo(Ator ator, @PathVariable UUID id, @Valid @RequestBody MotivoRequest corpo) {
        prevencao.registrarMotivo(ator, id, corpo.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/consultas/{id}/ligacao")
    @Operation(summary = "Registra o resultado de uma ligação de prevenção", description = "CONFIRMOU também confirma a presença na consulta.")
    public ResponseEntity<Void> registrarLigacao(Ator ator, @PathVariable UUID id, @Valid @RequestBody LigacaoRequest corpo) {
        prevencao.registrarLigacao(ator, id, corpo.resultado());
        return ResponseEntity.noContent().build();
    }
}
