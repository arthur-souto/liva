package sus.liva.domain.model.cuidador;

import sus.liva.domain.model.Telefone;

import java.util.UUID;

public record Cuidador(UUID id, String nome, String cpf, Telefone telefone) {}
