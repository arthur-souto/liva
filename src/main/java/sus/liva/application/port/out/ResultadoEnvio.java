package sus.liva.application.port.out;

public record ResultadoEnvio(boolean sucesso, String idExterno, String erro) {
    public static ResultadoEnvio ok(String id) { return new ResultadoEnvio(true, id, null); }
    public static ResultadoEnvio falha(String erro) { return new ResultadoEnvio(false, null, erro); }
}
