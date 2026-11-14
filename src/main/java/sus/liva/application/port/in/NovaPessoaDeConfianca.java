package sus.liva.application.port.in;

import sus.liva.domain.model.cuidador.Parentesco;

public record NovaPessoaDeConfianca(String nome, String cpf, String telefone, Parentesco parentesco, boolean autorizacaoRegistrada) {}
