package sus.liva.application.port.in;

public record ResumoDoCiclo(int entradas, int vagas, int ofertas, int expiradas, int avisos, int prazos) {
    public boolean vazio() {
        return entradas + vagas + ofertas + expiradas + avisos + prazos == 0;
    }
}
