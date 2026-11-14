package sus.liva.domain.model;

public record Telefone(String e164) {
    public Telefone {
        if (e164 == null || !e164.matches("\\+55\\d{10,11}"))
            throw new IllegalArgumentException("Telefone inválido: " + e164);
    }
    public static Telefone de(String bruto) {
        String d = bruto == null ? "" : bruto.replaceAll("\\D", "");
        if (d.length() == 10 || d.length() == 11) d = "55" + d;
        return new Telefone("+" + d);
    }

    public String apenasDigitos() {
        return e164.substring(1);
    }

    public String chaveComparacao() {
        return e164.substring(3, 5) + e164.substring(e164.length() - 8);
    }
}