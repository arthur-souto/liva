package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.CadastroDePaciente;
import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pacientes")
public class PacienteEntity {
    @Id
    private UUID id;
    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;
    @Column(name = "nome_social", length = 150)
    private String nomeSocial;
    @Column(length = 11)
    private String cpf;
    @Column(name = "cartao_sus", length = 15)
    private String cartaoSus;
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sexo sexo;
    @Column(length = 150)
    private String email;
    @Column(nullable = false, length = 20)
    private String telefone;
    @Column(length = 9)
    private String cep;
    @Column(length = 150)
    private String logradouro;
    @Column(length = 10)
    private String numero;
    @Column(length = 100)
    private String complemento;
    @Column(length = 100)
    private String bairro;
    @Column(length = 100)
    private String cidade;
    @Column(length = 2)
    private String uf;
    @Column(name = "nome_cuidador", length = 150)
    private String nomeCuidador;
    @Column(name = "telefone_cuidador", length = 20)
    private String telefoneCuidador;
    @Column(name = "contato_emergencia_nome", length = 150)
    private String contatoEmergenciaNome;
    @Column(name = "contato_emergencia_telefone", length = 20)
    private String contatoEmergenciaTelefone;
    @Column(length = 500)
    private String observacoes;
    @Column(nullable = false)
    private boolean consentimento;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;
    @Column(name = "criado_por", updatable = false)
    private UUID criadoPor;
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;
    @Column(name = "atualizado_por")
    private UUID atualizadoPor;
    @Version
    private Long version;

    protected PacienteEntity() {
    }

    static PacienteEntity novo(Paciente p, UUID responsavel, Instant em) {
        PacienteEntity e = new PacienteEntity();
        e.id = p.id();
        e.criadoEm = em;
        e.criadoPor = responsavel;
        e.copiar(p);
        return e;
    }

    void copiar(Paciente p) {
        nomeCompleto = p.nomeCompleto();
        nomeSocial = p.nomeSocial();
        cpf = p.cpf();
        cartaoSus = p.cartaoSus();
        dataNascimento = p.dataNascimento();
        sexo = p.sexo();
        email = p.email();
        telefone = e164(p.telefone());
        Endereco end = p.endereco();
        cep = end == null ? null : end.cep();
        logradouro = end == null ? null : end.logradouro();
        numero = end == null ? null : end.numero();
        complemento = end == null ? null : end.complemento();
        bairro = end == null ? null : end.bairro();
        cidade = end == null ? null : end.cidade();
        uf = end == null ? null : end.uf();
        nomeCuidador = p.nomeCuidador();
        telefoneCuidador = e164(p.telefoneCuidador());
        contatoEmergenciaNome = p.contatoEmergenciaNome();
        contatoEmergenciaTelefone = e164(p.contatoEmergenciaTelefone());
        observacoes = p.observacoes();
        consentimento = p.consentimento();
    }

    void registrarAtualizacao(UUID responsavel, Instant em) {
        atualizadoPor = responsavel;
        atualizadoEm = em;
    }

    Long getVersion() {
        return version;
    }

    CadastroDePaciente paraCadastro() {
        return new CadastroDePaciente(paraDominio(), version, criadoEm, criadoPor, atualizadoEm);
    }

    Paciente paraDominio() {
        return new Paciente(id, nomeCompleto, nomeSocial, cpf, cartaoSus, dataNascimento, sexo, email,
                telefone(telefone), endereco(), nomeCuidador, telefone(telefoneCuidador), contatoEmergenciaNome,
                telefone(contatoEmergenciaTelefone), observacoes, consentimento);
    }

    private Endereco endereco() {
        if (cep == null && logradouro == null && numero == null && complemento == null
                && bairro == null && cidade == null && uf == null) return null;
        return new Endereco(cep, logradouro, numero, complemento, bairro, cidade, uf);
    }

    private static String e164(Telefone t) {
        return t == null ? null : t.e164();
    }

    private static Telefone telefone(String e164) {
        return e164 == null ? null : new Telefone(e164);
    }
}
