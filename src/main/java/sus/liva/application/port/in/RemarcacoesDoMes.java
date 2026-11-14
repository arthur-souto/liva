package sus.liva.application.port.in;

public record RemarcacoesDoMes(int limite, int usadas) {
    public int restantes() {
        return Math.max(0, limite - usadas);
    }
}
