package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.out.ContaDeAcesso;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 150)
    private String login;
    @Column(nullable = false, length = 150)
    private String nome;
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Papel papel;
    @Column(name = "profissional_id", updatable = false)
    private UUID profissionalId;
    @Column(name = "paciente_id", updatable = false)
    private UUID pacienteId;
    @Column(nullable = false)
    private boolean ativo;
    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;
    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;
    @Version
    private Long version;

    protected UsuarioEntity() {
    }

    static UsuarioEntity nova(ContaDeAcesso c, Instant em) {
        UsuarioEntity n = new UsuarioEntity();
        n.id = c.id();
        n.login = c.login();
        n.nome = c.nome();
        n.senhaHash = c.senhaHash();
        n.papel = c.papel();
        n.profissionalId = c.profissionalId();
        n.pacienteId = c.pacienteId();
        n.ativo = c.ativo();
        n.criadoEm = em;
        return n;
    }

    void redefinirSenha(String hash) {
        senhaHash = hash;
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    ContaDeAcesso paraConta() {
        return new ContaDeAcesso(id, login, nome, senhaHash, papel, profissionalId, pacienteId, ativo, tentativasFalhas, bloqueadoAte);
    }
}
