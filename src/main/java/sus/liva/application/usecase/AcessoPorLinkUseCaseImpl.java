package sus.liva.application.usecase;

import sus.liva.application.exception.NaoAutenticadoException;
import sus.liva.application.port.in.AcessoPorLink;
import sus.liva.application.port.in.AcessoPorLinkUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.Sessao;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.ContaDeAcesso;
import sus.liva.application.port.out.ContaDeAcessoPort;
import sus.liva.application.port.out.EmissorDeTokenPort;
import sus.liva.application.port.out.LinkDeAcesso;
import sus.liva.application.port.out.LinkDeAcessoPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.TokenEmitido;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class AcessoPorLinkUseCaseImpl implements AcessoPorLinkUseCase {
    private static final Duration SESSAO = Duration.ofHours(2);
    private static final String LINK_INVALIDO = "Este link expirou ou já foi usado. Peça um novo pelo WhatsApp enviando MENU.";

    private final LinkDeAcessoPort links;
    private final ContaDeAcessoPort contas;
    private final PacienteRepositoryPort pacientes;
    private final EmissorDeTokenPort emissor;
    private final TransacaoPort transacao;
    private final Auditor auditor;
    private final Clock clock;

    public AcessoPorLinkUseCaseImpl(LinkDeAcessoPort links, ContaDeAcessoPort contas, PacienteRepositoryPort pacientes,
                                    EmissorDeTokenPort emissor, TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        this.links = links;
        this.contas = contas;
        this.pacientes = pacientes;
        this.emissor = emissor;
        this.transacao = transacao;
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
    }

    @Override
    public AcessoPorLink entrar(String token) {
        if (token == null || token.isBlank() || token.length() > 100) throw new NaoAutenticadoException(LINK_INVALIDO);

        String hash = LinksDeAcesso.hash(token.trim());
        Instant agora = Instant.now(clock);

        LinkDeAcesso link = transacao.executar(() -> {
            Optional<LinkDeAcesso> valido = links.buscarParaUsar(hash).filter(l -> l.validoEm(agora));
            valido.ifPresent(l -> links.marcarUsado(hash, agora));
            return valido;
        }).orElseThrow(() -> new NaoAutenticadoException(LINK_INVALIDO));

        Paciente paciente = pacientes.buscarPaciente(link.pacienteId()).orElseThrow(() -> new NaoAutenticadoException(LINK_INVALIDO));
        Optional<ContaDeAcesso> conta = contas.buscarPorPaciente(paciente.id());
        if (conta.isPresent() && !conta.get().ativo())
            throw new NaoAutenticadoException("Este acesso está desativado. Procure a recepção da unidade.");

        Ator ator = conta.map(ContaDeAcesso::ator).orElse(Ator.paciente(paciente.id(), paciente.id()));
        String nome = conta.map(ContaDeAcesso::nome)
                .orElse(paciente.nomeSocial() != null ? paciente.nomeSocial() : paciente.nomeCompleto());
        TokenEmitido emitido = emissor.emitir(ator, nome, SESSAO);

        auditor.registrar(ator, "ENTRAR_POR_LINK", "PACIENTE", paciente.id(), paciente.id());
        return new AcessoPorLink(new Sessao(emitido.token(), emitido.expiraEm(), ator, nome), link.destino());
    }
}
