package sus.liva.application.port.out;

public interface CifradorPort {
    String cifrar(String segredo);
    boolean confere(String segredo, String cifrado);
}
