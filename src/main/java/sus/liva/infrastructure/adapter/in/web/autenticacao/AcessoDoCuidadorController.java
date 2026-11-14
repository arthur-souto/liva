package sus.liva.infrastructure.adapter.in.web.autenticacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.PessoaDeConfiancaUseCase;
import sus.liva.infrastructure.adapter.in.web.autenticacao.AutenticacaoController.SessaoResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/cuidador")
@Tag(name = "Acesso", description = "Login da equipe e ativação do portal do paciente")
public class AcessoDoCuidadorController {
    private static final String CODIGO_SOLICITADO = "Se os dados conferirem, enviaremos um código para o WhatsApp da pessoa de confiança.";

    private final PessoaDeConfiancaUseCase pessoasDeConfianca;

    public AcessoDoCuidadorController(PessoaDeConfiancaUseCase pessoasDeConfianca) {
        this.pessoasDeConfianca = pessoasDeConfianca;
    }

    public record SolicitacaoDeCodigo(String cpfDoCuidador, String cpfDoPaciente) {}

    public record Ativacao(String cpfDoCuidador, String cpfDoPaciente, String codigo, String senha) {}

    public record Credenciais(String cpfDoCuidador, String cpfDoPaciente, String senha) {}

    @PostMapping("/codigo")
    @Operation(summary = "Pessoa de confiança solicita o código de primeiro acesso pelo WhatsApp",
            description = "A resposta é sempre a mesma para não revelar quem está cadastrado.")
    public ResponseEntity<Map<String, String>> solicitarCodigo(@RequestBody SolicitacaoDeCodigo r) {
        pessoasDeConfianca.solicitarCodigo(r.cpfDoCuidador(), r.cpfDoPaciente());
        return ResponseEntity.accepted().body(Map.of("mensagem", CODIGO_SOLICITADO));
    }

    @PostMapping("/ativar")
    @Operation(summary = "Cria (ou redefine) a senha da pessoa de confiança com o código recebido")
    public SessaoResponse ativar(@RequestBody Ativacao r) {
        return SessaoResponse.de(pessoasDeConfianca.ativar(r.cpfDoCuidador(), r.cpfDoPaciente(), r.codigo(), r.senha()));
    }

    @PostMapping("/entrar")
    @Operation(summary = "Pessoa de confiança entra com o próprio CPF, o CPF de quem cuida e a senha",
            description = "Após 5 senhas erradas o acesso fica bloqueado por 15 minutos.")
    public SessaoResponse entrar(@RequestBody Credenciais r) {
        return SessaoResponse.de(pessoasDeConfianca.entrar(r.cpfDoCuidador(), r.cpfDoPaciente(), r.senha()));
    }
}
