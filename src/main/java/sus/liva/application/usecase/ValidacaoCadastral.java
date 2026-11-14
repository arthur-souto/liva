package sus.liva.application.usecase;

import sus.liva.domain.model.Telefone;

import java.text.Normalizer;
import java.util.Locale;

final class ValidacaoCadastral {
    private ValidacaoCadastral() {
    }

    static String cpf(String valor) {
        String d = valor.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1 || !digitosConferem(d))
            throw new IllegalArgumentException("CPF inválido: \"" + valor + "\"");
        return d;
    }

    static String cartaoSus(String valor) {
        String d = valor.replaceAll("\\D", "");
        if (d.length() != 15) throw new IllegalArgumentException("Cartão SUS inválido: deve ter 15 dígitos");
        return d;
    }

    static Telefone telefone(String valor, String campo) {
        try {
            return Telefone.de(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(campo + " inválido: \"" + valor + "\". Use DDD + número, ex.: 51999999999");
        }
    }

    static String normalizar(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    static void exigir(String valor, String mensagem) {
        if (vazio(valor)) throw new IllegalArgumentException(mensagem);
    }

    static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    static String opcional(String valor) {
        return vazio(valor) ? null : valor.trim();
    }

    private static boolean digitosConferem(String d) {
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) soma += (d.charAt(i) - '0') * (posicao + 1 - i);
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
