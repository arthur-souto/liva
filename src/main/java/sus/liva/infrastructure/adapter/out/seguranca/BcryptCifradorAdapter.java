package sus.liva.infrastructure.adapter.out.seguranca;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import sus.liva.application.port.out.CifradorPort;

@Component
public class BcryptCifradorAdapter implements CifradorPort {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);

    @Override
    public String cifrar(String segredo) {
        return bcrypt.encode(segredo);
    }

    @Override
    public boolean confere(String segredo, String cifrado) {
        return segredo != null && cifrado != null && bcrypt.matches(segredo, cifrado);
    }
}
