package sus.liva.application.usecase;

import sus.liva.application.exception.ConflitoException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.CadastroDePacienteUseCase;
import sus.liva.application.port.in.DadosCadastrais;
import sus.liva.application.port.in.Pagina;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.CadastroDePaciente;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static sus.liva.application.usecase.ValidacaoCadastral.cartaoSus;
import static sus.liva.application.usecase.ValidacaoCadastral.cpf;
import static sus.liva.application.usecase.ValidacaoCadastral.exigir;
import static sus.liva.application.usecase.ValidacaoCadastral.opcional;
import static sus.liva.application.usecase.ValidacaoCadastral.telefone;
import static sus.liva.application.usecase.ValidacaoCadastral.vazio;

public class CadastroDePacienteUseCaseImpl implements CadastroDePacienteUseCase {
    private static final int LIMITE_PESQUISA = 20;
    private static final int MINIMO_NOME = 3;
    private static final int LIMITE_RECENTES = 50;

    private final PacienteRepositoryPort pacientes;
    private final Auditor auditor;
    private final Clock clock;

    public CadastroDePacienteUseCaseImpl(PacienteRepositoryPort pacientes, AuditoriaPort auditoria, Clock clock) {
        this.pacientes = pacientes;
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
    }

    @Override
    public CadastroDePaciente cadastrar(Ator ator, DadosCadastrais dados) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.ADMINISTRADOR);
        Paciente paciente = montar(UUID.randomUUID(), dados);
        if (paciente.cpf() == null && paciente.cartaoSus() == null)
            throw new IllegalArgumentException("Informe o CPF ou o Cartão SUS para identificar o paciente");
        recusarDuplicado(paciente, null);
        CadastroDePaciente cadastro = pacientes.cadastrar(paciente, ator.usuarioId(), Instant.now(clock));
        auditor.registrar(ator, "CADASTRAR_PACIENTE", "PACIENTE", paciente.id(), paciente.id());
        return cadastro;
    }

    @Override
    public CadastroDePaciente atualizar(Ator ator, UUID pacienteId, DadosCadastrais dados, long versao) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.ADMINISTRADOR);
        CadastroDePaciente atual = existente(pacienteId);
        if (atual.versao() != versao)
            throw new ConflitoException("O cadastro foi alterado por outra pessoa. Recarregue antes de continuar.");
        Paciente paciente = montar(pacienteId, dados);
        if (paciente.cpf() == null && paciente.cartaoSus() == null)
            throw new IllegalArgumentException("Informe o CPF ou o Cartão SUS para identificar o paciente");
        if (atual.paciente().cpf() != null && paciente.cpf() == null)
            throw new IllegalArgumentException("O CPF já registrado não pode ser removido");
        recusarDuplicado(paciente, pacienteId);
        CadastroDePaciente cadastro = pacientes.atualizarCadastro(paciente, versao, ator.usuarioId(), Instant.now(clock));
        auditor.registrar(ator, "ATUALIZAR_PACIENTE", "PACIENTE", pacienteId, pacienteId);
        return cadastro;
    }

    @Override
    public CadastroDePaciente buscar(Ator ator, UUID pacienteId) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.ADMINISTRADOR, Papel.CLINICO_GERAL, Papel.ESPECIALISTA, Papel.PACIENTE);
        Permissoes.exigirPacienteProprio(ator, pacienteId);
        CadastroDePaciente cadastro = existente(pacienteId);
        auditor.registrar(ator, "CONSULTAR_PACIENTE", "PACIENTE", pacienteId, pacienteId);
        return cadastro;
    }

    @Override
    public List<CadastroDePaciente> pesquisar(Ator ator, String termo) {
        int limite = termo == null || termo.isBlank() ? LIMITE_RECENTES : LIMITE_PESQUISA;
        return pesquisar(ator, termo, new PedidoDePagina(1, limite)).itens();
    }

    @Override
    public Pagina<CadastroDePaciente> pesquisar(Ator ator, String termo, PedidoDePagina pedido) {
        Permissoes.exigir(ator, Papel.RECEPCAO, Papel.ADMINISTRADOR, Papel.CLINICO_GERAL);
        String limpo = termo == null ? "" : termo.trim();
        if (limpo.isEmpty()) return pacientes.cadastrosRecentes(pedido);
        String digitos = limpo.replaceAll("[\\s.\\-/]", "");
        if (digitos.matches("\\d{11}|\\d{15}")) return pacientes.pesquisarCadastros(null, digitos, pedido);
        if (limpo.length() < MINIMO_NOME)
            throw new IllegalArgumentException("Digite ao menos " + MINIMO_NOME + " letras do nome, o CPF ou o Cartão SUS");
        return pacientes.pesquisarCadastros(limpo, null, pedido);
    }

    private CadastroDePaciente existente(UUID pacienteId) {
        return pacientes.buscarCadastro(pacienteId)
                .orElseThrow(() -> new NaoEncontradoException("Paciente não encontrado: " + pacienteId));
    }

    private void recusarDuplicado(Paciente paciente, UUID proprio) {
        Optional<Paciente> porCpf = paciente.cpf() == null ? Optional.empty() : pacientes.buscarPorCpf(paciente.cpf());
        if (porCpf.filter(p -> !p.id().equals(proprio)).isPresent())
            throw new ConflitoException("Já existe um paciente cadastrado com este CPF.");
        Optional<Paciente> porCns = paciente.cartaoSus() == null ? Optional.empty() : pacientes.buscarPorCartaoSus(paciente.cartaoSus());
        if (porCns.filter(p -> !p.id().equals(proprio)).isPresent())
            throw new ConflitoException("Já existe um paciente cadastrado com este Cartão SUS.");
    }

    private Paciente montar(UUID id, DadosCadastrais d) {
        exigir(d.nomeCompleto(), "Nome completo é obrigatório");
        if (d.dataNascimento() != null && d.dataNascimento().isAfter(LocalDate.now(clock)))
            throw new IllegalArgumentException("Data de nascimento não pode estar no futuro");
        Telefone telefoneCuidador = vazio(d.telefoneCuidador()) ? null : telefone(d.telefoneCuidador(), "Telefone do cuidador");
        if (telefoneCuidador != null) exigir(d.nomeCuidador(), "Nome do cuidador é obrigatório quando há telefone do cuidador");
        Telefone telefoneEmergencia = vazio(d.contatoEmergenciaTelefone()) ? null
                : telefone(d.contatoEmergenciaTelefone(), "Telefone de emergência");
        return new Paciente(id, d.nomeCompleto().trim(), opcional(d.nomeSocial()),
                vazio(d.cpf()) ? null : cpf(d.cpf()), vazio(d.cartaoSus()) ? null : cartaoSus(d.cartaoSus()),
                d.dataNascimento(), d.sexo() == null ? Sexo.NAO_INFORMADO : d.sexo(), opcional(d.email()),
                telefone(d.telefone(), "Telefone do paciente"), d.endereco(),
                telefoneCuidador == null ? null : d.nomeCuidador().trim(), telefoneCuidador,
                opcional(d.contatoEmergenciaNome()), telefoneEmergencia, opcional(d.observacoes()), d.consentimento());
    }
}
