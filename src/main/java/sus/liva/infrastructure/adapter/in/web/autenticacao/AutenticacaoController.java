package sus.liva.infrastructure.adapter.in.web.autenticacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.exception.NaoAutenticadoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.AutenticacaoUseCase;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.Sessao;
import sus.liva.infrastructure.adapter.in.web.seguranca.AtorArgumentResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Acesso", description = "Login da equipe e ativação do portal do paciente")
public class AutenticacaoController {
    private static final String CODIGO_SOLICITADO = "Se os dados conferirem e já houver uma triagem concluída, "
            + "enviaremos um código para o WhatsApp cadastrado.";

    private final AutenticacaoUseCase autenticacao;

    public AutenticacaoController(AutenticacaoUseCase autenticacao) {
        this.autenticacao = autenticacao;
    }

    public record Credenciais(String login, String senha) {}

    public record SolicitacaoDeCodigo(String documento, LocalDate dataNascimento) {}

    public record Ativacao(String documento, LocalDate dataNascimento, String codigo, String senha) {}

    public record Usuario(UUID id, String nome, Papel papel, UUID profissionalId, UUID pacienteId) {
        static Usuario de(Ator ator, String nome) {
            return new Usuario(ator.usuarioId(), nome, ator.papel(), ator.profissionalId(), ator.pacienteId());
        }
    }

    public record SessaoResponse(String token, Instant expiraEm, Usuario usuario) {
        static SessaoResponse de(Sessao s) {
            return new SessaoResponse(s.token(), s.expiraEm(), Usuario.de(s.ator(), s.nome()));
        }
    }

    @PostMapping("/entrar")
    @Operation(summary = "Entra com e-mail (equipe) ou CPF/Cartão SUS (paciente) e senha",
            description = "Após 5 senhas erradas o acesso fica bloqueado por 15 minutos.")
    public SessaoResponse entrar(@RequestBody Credenciais credenciais) {
        return SessaoResponse.de(autenticacao.entrar(credenciais.login(), credenciais.senha()));
    }

    @PostMapping("/portal/codigo")
    @Operation(summary = "Solicita o código de ativação do portal pelo WhatsApp",
            description = "A resposta é sempre a mesma para não revelar quem é paciente. "
                    + "Só envia se a data de nascimento conferir e houver triagem concluída.")
    public ResponseEntity<Map<String, String>> solicitarCodigo(@RequestBody SolicitacaoDeCodigo request) {
        autenticacao.solicitarCodigoDoPortal(request.documento(), request.dataNascimento());
        return ResponseEntity.accepted().body(Map.of("mensagem", CODIGO_SOLICITADO));
    }

    @PostMapping("/portal/ativar")
    @Operation(summary = "Ativa o portal (ou redefine a senha) com o código recebido")
    public SessaoResponse ativar(@RequestBody Ativacao request) {
        return SessaoResponse.de(autenticacao.ativarPortal(request.documento(), request.dataNascimento(), request.codigo(),
                request.senha()));
    }

    @GetMapping("/eu")
    @Operation(summary = "Dados do usuário da sessão atual")
    public Usuario eu(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new NaoAutenticadoException("Entre com um usuário para continuar");
        return Usuario.de(AtorArgumentResolver.deToken(jwt), jwt.getClaimAsString(AtorArgumentResolver.NOME));
    }
}
