package sus.liva.application.usecase;

import sus.liva.application.exception.NaoAutenticadoException;
import sus.liva.application.port.in.AutenticacaoUseCase;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.Sessao;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.CifradorPort;
import sus.liva.application.port.out.CodigoDoPortal;
import sus.liva.application.port.out.CodigoDoPortalPort;
import sus.liva.application.port.out.ContaDeAcesso;
import sus.liva.application.port.out.ContaDeAcessoPort;
import sus.liva.application.port.out.EmissorDeTokenPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.application.port.out.TokenEmitido;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.port.out.TriagemRepositoryPort;
import sus.liva.domain.model.paciente.Paciente;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static sus.liva.application.usecase.ValidacaoCadastral.vazio;

public class AutenticacaoUseCaseImpl implements AutenticacaoUseCase {
    private static final System.Logger LOG = System.getLogger(AutenticacaoUseCaseImpl.class.getName());
    private static final int LIMITE_DE_TENTATIVAS = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final Duration SESSAO_DA_EQUIPE = Duration.ofHours(8);
    private static final Duration SESSAO_DO_PACIENTE = Duration.ofHours(2);
    private static final Duration VALIDADE_DO_CODIGO = Duration.ofMinutes(10);
    private static final Duration INTERVALO_ENTRE_CODIGOS = Duration.ofSeconds(60);
    private static final String CREDENCIAIS_INVALIDAS = "Login ou senha inválidos";
    private static final String CODIGO_INVALIDO = "Código inválido ou expirado. Solicite um novo código.";

    private final ContaDeAcessoPort contas;
    private final CodigoDoPortalPort codigos;
    private final PacienteRepositoryPort pacientes;
    private final TriagemRepositoryPort triagens;
    private final CifradorPort cifrador;
    private final EmissorDeTokenPort emissor;
    private final EnviadorMensagemPort enviador;
    private final TransacaoPort transacao;
    private final Auditor auditor;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();
    private final String hashDeReferencia;

    public AutenticacaoUseCaseImpl(ContaDeAcessoPort contas, CodigoDoPortalPort codigos, PacienteRepositoryPort pacientes,
                                   TriagemRepositoryPort triagens, CifradorPort cifrador, EmissorDeTokenPort emissor,
                                   EnviadorMensagemPort enviador, TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        this.contas = contas;
        this.codigos = codigos;
        this.pacientes = pacientes;
        this.triagens = triagens;
        this.cifrador = cifrador;
        this.emissor = emissor;
        this.enviador = enviador;
        this.transacao = transacao;
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
        this.hashDeReferencia = cifrador.cifrar(UUID.randomUUID().toString());
    }

    @Override
    public Sessao entrar(String login, String senha) {
        if (vazio(login) || vazio(senha)) throw new IllegalArgumentException("Informe o login e a senha");
        Optional<ContaDeAcesso> encontrada = conta(login);
        if (encontrada.isEmpty()) {
            cifrador.confere(senha, hashDeReferencia);
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        ContaDeAcesso conta = encontrada.get();
        Instant agora = Instant.now(clock);
        if (conta.bloqueadaEm(agora))
            throw new NaoAutenticadoException("Acesso bloqueado temporariamente por excesso de tentativas. Tente novamente mais tarde.");
        if (!cifrador.confere(senha, conta.senhaHash())) {
            contas.registrarFalha(conta.id(), LIMITE_DE_TENTATIVAS, agora.plus(BLOQUEIO));
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        if (!conta.ativo()) throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        if (conta.papel() == Papel.PACIENTE && !triagens.possuiTriagemConcluida(conta.pacienteId()))
            throw new NaoAutenticadoException("O portal é liberado após a primeira triagem concluída");
        contas.registrarAcesso(conta.id());
        auditor.registrar(conta.ator(), "ENTRAR", "USUARIO", conta.id(), conta.pacienteId());
        return sessao(conta);
    }

    @Override
    public void solicitarCodigoDoPortal(String documento, LocalDate dataNascimento) {
        Optional<Paciente> encontrado = paciente(documento, dataNascimento);
        if (encontrado.isEmpty()) return;
        Paciente paciente = encontrado.get();
        if (!paciente.consentimento() || !triagens.possuiTriagemConcluida(paciente.id())) return;

        String codigo = "%06d".formatted(aleatorio.nextInt(1_000_000));
        Instant agora = Instant.now(clock);
        boolean emitido = transacao.executar(() -> {
            boolean recente = codigos.buscarParaAtualizar(paciente.id())
                    .filter(c -> c.enviadoEm().plus(INTERVALO_ENTRE_CODIGOS).isAfter(agora))
                    .isPresent();
            if (recente) return false;
            codigos.salvarCodigo(new CodigoDoPortal(paciente.id(), cifrador.cifrar(codigo), agora.plus(VALIDADE_DO_CODIGO), agora, 0));
            return true;
        });
        if (!emitido) return;

        ResultadoEnvio envio = enviador.enviar(new MensagemSaida(null, paciente.telefone(), null,
                "ConfirmaSUS: seu código de acesso ao portal é " + codigo + ". Ele vale por "
                        + VALIDADE_DO_CODIGO.toMinutes() + " minutos. Não compartilhe este código com ninguém."));
        if (!envio.sucesso())
            LOG.log(System.Logger.Level.WARNING, "Código do portal não enviado ao paciente " + paciente.id() + ": " + envio.erro());
    }

    @Override
    public Sessao ativarPortal(String documento, LocalDate dataNascimento, String codigo, String senha) {
        exigirSenhaForte(senha);
        if (vazio(codigo) || !codigo.trim().matches("\\d{6}")) throw new IllegalArgumentException(CODIGO_INVALIDO);
        Paciente paciente = paciente(documento, dataNascimento).orElseThrow(() -> new IllegalArgumentException(CODIGO_INVALIDO));
        Instant agora = Instant.now(clock);

        Optional<ContaDeAcesso> ativada = transacao.executar(() -> {
            Optional<CodigoDoPortal> registro = codigos.buscarParaAtualizar(paciente.id());
            if (registro.isEmpty()) return Optional.empty();
            CodigoDoPortal atual = registro.get();
            if (!atual.expiraEm().isAfter(agora) || atual.tentativas() >= LIMITE_DE_TENTATIVAS) {
                codigos.removerCodigo(paciente.id());
                return Optional.empty();
            }
            if (!cifrador.confere(codigo.trim(), atual.codigoHash())) {
                codigos.salvarCodigo(new CodigoDoPortal(atual.pacienteId(), atual.codigoHash(), atual.expiraEm(),
                        atual.enviadoEm(), atual.tentativas() + 1));
                return Optional.empty();
            }
            if (!triagens.possuiTriagemConcluida(paciente.id())) return Optional.empty();
            codigos.removerCodigo(paciente.id());
            return Optional.of(contaDoPortal(paciente, cifrador.cifrar(senha), agora));
        });

        ContaDeAcesso conta = ativada.orElseThrow(() -> new IllegalArgumentException(CODIGO_INVALIDO));
        if (!conta.ativo()) throw new NaoAutenticadoException("Este acesso está desativado. Procure a recepção da unidade.");
        auditor.registrar(conta.ator(), "ATIVAR_PORTAL", "USUARIO", conta.id(), paciente.id());
        return sessao(conta);
    }

    private ContaDeAcesso contaDoPortal(Paciente paciente, String senhaHash, Instant agora) {
        Optional<ContaDeAcesso> existente = contas.buscarPorPaciente(paciente.id());
        if (existente.isPresent()) {
            contas.redefinirSenha(existente.get().id(), senhaHash);
            return existente.get();
        }
        ContaDeAcesso nova = new ContaDeAcesso(UUID.randomUUID(), paciente.cpf() != null ? paciente.cpf() : paciente.cartaoSus(),
                paciente.nomeSocial() != null ? paciente.nomeSocial() : paciente.nomeCompleto(), senhaHash, Papel.PACIENTE,
                null, paciente.id(), true, 0, null);
        contas.criarConta(nova, agora);
        return nova;
    }

    private Sessao sessao(ContaDeAcesso conta) {
        Duration validade = conta.papel() == Papel.PACIENTE ? SESSAO_DO_PACIENTE : SESSAO_DA_EQUIPE;
        TokenEmitido token = emissor.emitir(conta.ator(), conta.nome(), validade);
        return new Sessao(token.token(), token.expiraEm(), conta.ator(), conta.nome());
    }

    private Optional<ContaDeAcesso> conta(String login) {
        String limpo = login.trim();
        String digitos = limpo.replaceAll("[\\s.\\-/]", "");
        if (!digitos.matches("\\d{11}|\\d{15}")) return contas.buscarPorLogin(limpo.toLowerCase(Locale.ROOT));
        return contas.buscarPorLogin(digitos).or(() -> pacientePorDocumento(digitos).flatMap(p -> contas.buscarPorPaciente(p.id())));
    }

    private Optional<Paciente> paciente(String documento, LocalDate dataNascimento) {
        if (vazio(documento) || dataNascimento == null)
            throw new IllegalArgumentException("Informe o CPF ou Cartão SUS e a data de nascimento");
        String digitos = documento.replaceAll("[\\s.\\-/]", "");
        if (!digitos.matches("\\d{11}|\\d{15}")) throw new IllegalArgumentException("Informe um CPF (11 dígitos) ou Cartão SUS (15 dígitos)");
        return pacientePorDocumento(digitos).filter(p -> dataNascimento.equals(p.dataNascimento()));
    }

    private Optional<Paciente> pacientePorDocumento(String digitos) {
        return digitos.length() == 11 ? pacientes.buscarPorCpf(digitos) : pacientes.buscarPorCartaoSus(digitos);
    }

    private static void exigirSenhaForte(String senha) {
        if (senha == null || senha.length() < 8 || senha.length() > 72 || !senha.matches(".*\\p{L}.*") || !senha.matches(".*\\d.*"))
            throw new IllegalArgumentException("A senha deve ter de 8 a 72 caracteres, com letras e números");
    }
}
