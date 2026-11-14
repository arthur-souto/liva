package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

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

    protected PacienteEntity() {
    }

    static PacienteEntity de(Paciente p) {
        PacienteEntity e = new PacienteEntity();
        e.id = p.id();
        e.nomeCompleto = p.nomeCompleto();
        e.nomeSocial = p.nomeSocial();
        e.cpf = p.cpf();
        e.cartaoSus = p.cartaoSus();
        e.dataNascimento = p.dataNascimento();
        e.sexo = p.sexo();
        e.email = p.email();
        e.telefone = e164(p.telefone());
        Endereco end = p.endereco();
        if (end != null) {
            e.cep = end.cep();
            e.logradouro = end.logradouro();
            e.numero = end.numero();
            e.complemento = end.complemento();
            e.bairro = end.bairro();
            e.cidade = end.cidade();
            e.uf = end.uf();
        }
        e.nomeCuidador = p.nomeCuidador();
        e.telefoneCuidador = e164(p.telefoneCuidador());
        e.contatoEmergenciaNome = p.contatoEmergenciaNome();
        e.contatoEmergenciaTelefone = e164(p.contatoEmergenciaTelefone());
        e.observacoes = p.observacoes();
        e.consentimento = p.consentimento();
        return e;
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
