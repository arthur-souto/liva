package sus.liva.application.usecase;

import sus.liva.application.exception.NaoAutenticadoException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.NovaPessoaDeConfianca;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PessoaDeConfiancaUseCase;
import sus.liva.application.port.in.Sessao;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.CifradorPort;
import sus.liva.application.port.out.CodigoDoCuidador;
import sus.liva.application.port.out.CredencialDoCuidador;
import sus.liva.application.port.out.CuidadorRepositoryPort;
import sus.liva.application.port.out.EmissorDeTokenPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.application.port.out.TokenEmitido;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.port.out.TriagemRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.cuidador.Cuidador;
import sus.liva.domain.model.cuidador.CuidadorVinculado;
import sus.liva.domain.model.cuidador.VinculoDeCuidado;
import sus.liva.domain.model.paciente.Paciente;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static sus.liva.application.usecase.ValidacaoCadastral.cpf;
import static sus.liva.application.usecase.ValidacaoCadastral.exigir;
import static sus.liva.application.usecase.ValidacaoCadastral.telefone;
import static sus.liva.application.usecase.ValidacaoCadastral.vazio;

public class PessoaDeConfiancaUseCaseImpl implements PessoaDeConfiancaUseCase {
    private static final System.Logger LOG = System.getLogger(PessoaDeConfiancaUseCaseImpl.class.getName());
    private static final int LIMITE_DE_TENTATIVAS = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final Duration SESSAO = Duration.ofHours(2);
    private static final Duration VALIDADE_DO_CODIGO = Duration.ofMinutes(10);
    private static final Duration INTERVALO_ENTRE_CODIGOS = Duration.ofSeconds(60);
    private static final String CREDENCIAIS_INVALIDAS = "CPF, CPF da pessoa cuidada ou senha inválidos";
    private static final String CODIGO_INVALIDO = "Código inválido ou expirado. Solicite um novo código.";

    private final CuidadorRepositoryPort cuidadores;
    private final PacienteRepositoryPort pacientes;
    private final TriagemRepositoryPort triagens;
    private final CifradorPort cifrador;
    private final EmissorDeTokenPort emissor;
    private final EnviadorMensagemPort enviador;
    private final NotificacaoAoPaciente notificacao;
    private final ModeloMensagens modelos;
    private final TransacaoPort transacao;
    private final Auditor auditor;
    private final Clock clock;
    private final String urlDoPortal;
    private final SecureRandom aleatorio = new SecureRandom();
    private final String hashDeReferencia;

    public PessoaDeConfiancaUseCaseImpl(CuidadorRepositoryPort cuidadores, PacienteRepositoryPort pacientes, TriagemRepositoryPort triagens,
                                        CifradorPort cifrador, EmissorDeTokenPort emissor, EnviadorMensagemPort enviador,
                                        NotificacaoAoPaciente notificacao, ModeloMensagens modelos, TransacaoPort transacao,
                                        AuditoriaPort auditoria, Clock clock, String urlDoPortal) {
        this.cuidadores = cuidadores;
        this.pacientes = pacientes;
        this.triagens = triagens;
        this.cifrador = cifrador;
        this.emissor = emissor;
        this.enviador = enviador;
        this.notificacao = notificacao;
        this.modelos = modelos;
        this.transacao = transacao;
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
        this.urlDoPortal = urlDoPortal.endsWith("/") ? urlDoPortal.substring(0, urlDoPortal.length() - 1) : urlDoPortal;
        this.hashDeReferencia = cifrador.cifrar(UUID.randomUUID().toString());
    }

    @Override
    public CuidadorVinculado vincular(Ator ator, UUID pacienteId, NovaPessoaDeConfianca dados) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.ADMINISTRADOR);
        if (dados == null) throw new IllegalArgumentException("Informe os dados da pessoa de confiança");
        exigir(dados.nome(), "Nome da pessoa de confiança é obrigatório");
        if (vazio(dados.cpf())) throw new IllegalArgumentException("CPF da pessoa de confiança é obrigatório");
        String cpf = cpf(dados.cpf());
        Telefone telefone = telefone(dados.telefone(), "WhatsApp da pessoa de confiança");
        if (dados.parentesco() == null) throw new IllegalArgumentException("Informe o parentesco ou a relação com o paciente");
        if (!dados.autorizacaoRegistrada())
            throw new IllegalArgumentException("Registre que o paciente ou o responsável legal autorizou esta pessoa de confiança");
        Paciente paciente = pacientes.buscarPaciente(pacienteId)
                .orElseThrow(() -> new NaoEncontradoException("Paciente não encontrado: " + pacienteId));
        if (cpf.equals(paciente.cpf())) throw new IllegalArgumentException("A pessoa de confiança não pode ser o próprio paciente");

        Instant agora = Instant.now(clock);
        Cuidador cuidador = transacao.executar(() -> {
            UUID id = cuidadores.buscarCuidadorPorCpf(cpf).map(Cuidador::id).orElseGet(UUID::randomUUID);
            Cuidador salvo = new Cuidador(id, dados.nome().trim(), cpf, telefone);
            cuidadores.salvarCuidador(salvo, agora);
            cuidadores.vincular(new VinculoDeCuidado(id, pacienteId, dados.parentesco(), ator.usuarioId(), agora));
            return salvo;
        });
        auditor.registrar(ator, "VINCULAR_PESSOA_DE_CONFIANCA", "PACIENTE", pacienteId, pacienteId);
        notificacao.enviar(null, cuidador.telefone(), modelos.boasVindasAoCuidador(cuidador.nome(), paciente, urlDoPortal));
        return new CuidadorVinculado(cuidador, dados.parentesco(), agora);
    }

    @Override
    public void revogar(Ator ator, UUID pacienteId, UUID cuidadorId) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.ADMINISTRADOR);
        if (!cuidadores.cuidaDe(cuidadorId, pacienteId)) throw new NaoEncontradoException("Pessoa de confiança não encontrada");
        cuidadores.revogarVinculo(cuidadorId, pacienteId, Instant.now(clock));
        auditor.registrar(ator, "REVOGAR_PESSOA_DE_CONFIANCA", "PACIENTE", pacienteId, pacienteId);
    }

    @Override
    public List<CuidadorVinculado> doPaciente(Ator ator, UUID pacienteId) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.ADMINISTRADOR, Papel.PACIENTE, Papel.CUIDADOR);
        Permissoes.exigirPacienteProprio(ator, pacienteId);
        return cuidadores.cuidadoresDoPaciente(pacienteId);
    }

    @Override
    public void solicitarCodigo(String cpfDoCuidador, String cpfDoPaciente) {
        Optional<Vinculo> vinculo = vinculo(cpfDoCuidador, cpfDoPaciente);
        if (vinculo.isEmpty()) return;
        Cuidador cuidador = vinculo.get().cuidador();
        Paciente paciente = vinculo.get().paciente();

        String codigo = "%06d".formatted(aleatorio.nextInt(1_000_000));
        Instant agora = Instant.now(clock);
        boolean emitido = transacao.executar(() -> {
            boolean recente = cuidadores.codigoParaAtualizar(cuidador.id())
                    .filter(c -> c.enviadoEm().plus(INTERVALO_ENTRE_CODIGOS).isAfter(agora))
                    .isPresent();
            if (recente) return false;
            cuidadores.salvarCodigo(cuidador.id(), new CodigoDoCuidador(cifrador.cifrar(codigo), agora.plus(VALIDADE_DO_CODIGO), agora, 0));
            return true;
        });
        if (!emitido) return;

        ResultadoEnvio envio = enviador.enviar(new MensagemSaida(null, cuidador.telefone(), null,
                "ConfirmaSUS: seu código para acessar o portal como pessoa de confiança de " + paciente.nomeCompleto() + " é " + codigo
                        + ". Ele vale por " + VALIDADE_DO_CODIGO.toMinutes() + " minutos. Não compartilhe este código com ninguém."));
        if (!envio.sucesso())
            LOG.log(System.Logger.Level.WARNING, "Código da pessoa de confiança não enviado ao cuidador " + cuidador.id() + ": " + envio.erro());
    }

    @Override
    public Sessao ativar(String cpfDoCuidador, String cpfDoPaciente, String codigo, String senha) {
        exigirSenhaForte(senha);
        if (vazio(codigo) || !codigo.trim().matches("\\d{6}")) throw new IllegalArgumentException(CODIGO_INVALIDO);
        Vinculo vinculo = vinculo(cpfDoCuidador, cpfDoPaciente).orElseThrow(() -> new IllegalArgumentException(CODIGO_INVALIDO));
        UUID cuidadorId = vinculo.cuidador().id();
        Instant agora = Instant.now(clock);

        boolean valido = transacao.executar(() -> {
            Optional<CodigoDoCuidador> registro = cuidadores.codigoParaAtualizar(cuidadorId);
            if (registro.isEmpty()) return false;
            CodigoDoCuidador atual = registro.get();
            if (!atual.expiraEm().isAfter(agora) || atual.tentativas() >= LIMITE_DE_TENTATIVAS) {
                cuidadores.removerCodigo(cuidadorId);
                return false;
            }
            if (!cifrador.confere(codigo.trim(), atual.codigoHash())) {
                cuidadores.salvarCodigo(cuidadorId, new CodigoDoCuidador(atual.codigoHash(), atual.expiraEm(), atual.enviadoEm(),
                        atual.tentativas() + 1));
                return false;
            }
            cuidadores.removerCodigo(cuidadorId);
            cuidadores.definirSenha(cuidadorId, cifrador.cifrar(senha));
            return true;
        });
        if (!valido) throw new IllegalArgumentException(CODIGO_INVALIDO);
        Ator ator = Ator.cuidador(cuidadorId, vinculo.paciente().id());
        auditor.registrar(ator, "ATIVAR_ACESSO_DE_CUIDADOR", "PACIENTE", vinculo.paciente().id(), vinculo.paciente().id());
        return sessao(vinculo);
    }

    @Override
    public Sessao entrar(String cpfDoCuidador, String cpfDoPaciente, String senha) {
        if (vazio(cpfDoCuidador) || vazio(cpfDoPaciente) || vazio(senha))
            throw new IllegalArgumentException("Informe o seu CPF, o CPF da pessoa cuidada e a senha");
        Optional<Cuidador> cuidador = cuidadorPorCpf(cpfDoCuidador);
        Optional<CredencialDoCuidador> credencial = cuidador.flatMap(c -> cuidadores.credencial(c.id()));
        if (credencial.isEmpty()) {
            cifrador.confere(senha, hashDeReferencia);
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        Instant agora = Instant.now(clock);
        UUID cuidadorId = cuidador.get().id();
        if (credencial.get().bloqueadaEm(agora))
            throw new NaoAutenticadoException("Acesso bloqueado temporariamente por excesso de tentativas. Tente novamente mais tarde.");
        if (!cifrador.confere(senha, credencial.get().senhaHash())) {
            cuidadores.registrarFalha(cuidadorId, LIMITE_DE_TENTATIVAS, agora.plus(BLOQUEIO));
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        Vinculo vinculo = vinculo(cpfDoCuidador, cpfDoPaciente).orElseThrow(() -> new NaoAutenticadoException(CREDENCIAIS_INVALIDAS));
        cuidadores.registrarAcesso(cuidadorId);
        auditor.registrar(Ator.cuidador(cuidadorId, vinculo.paciente().id()), "ENTRAR", "PACIENTE", vinculo.paciente().id(),
                vinculo.paciente().id());
        return sessao(vinculo);
    }

    private Sessao sessao(Vinculo vinculo) {
        Ator ator = Ator.cuidador(vinculo.cuidador().id(), vinculo.paciente().id());
        TokenEmitido token = emissor.emitir(ator, vinculo.cuidador().nome(), SESSAO);
        return new Sessao(token.token(), token.expiraEm(), ator, vinculo.cuidador().nome());
    }

    private Optional<Vinculo> vinculo(String cpfDoCuidador, String cpfDoPaciente) {
        Optional<Cuidador> cuidador = cuidadorPorCpf(cpfDoCuidador);
        Optional<Paciente> paciente = digitos(cpfDoPaciente).flatMap(pacientes::buscarPorCpf);
        if (cuidador.isEmpty() || paciente.isEmpty()) return Optional.empty();
        if (!cuidadores.cuidaDe(cuidador.get().id(), paciente.get().id())) return Optional.empty();
        if (!triagens.possuiTriagemConcluida(paciente.get().id())) return Optional.empty();
        return Optional.of(new Vinculo(cuidador.get(), paciente.get()));
    }

    private Optional<Cuidador> cuidadorPorCpf(String cpf) {
        return digitos(cpf).flatMap(cuidadores::buscarCuidadorPorCpf);
    }

    private static Optional<String> digitos(String cpf) {
        if (cpf == null) return Optional.empty();
        String d = cpf.replaceAll("\\D", "");
        return d.length() == 11 ? Optional.of(d) : Optional.empty();
    }

    private static void exigirSenhaForte(String senha) {
        if (senha == null || senha.length() < 8 || senha.length() > 72 || !senha.matches(".*\\p{L}.*") || !senha.matches(".*\\d.*"))
            throw new IllegalArgumentException("A senha deve ter de 8 a 72 caracteres, com letras e números");
    }

    private record Vinculo(Cuidador cuidador, Paciente paciente) {}
}
