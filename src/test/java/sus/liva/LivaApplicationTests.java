package sus.liva;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import sus.liva.infrastructure.adapter.in.agendador.DisparoDiarioAgendador;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"confirmasus.whatsapp.provedor=console", "confirmasus.disparo-diario.cron=-",
        "confirmasus.seguranca.api-key=chave-de-teste",
        "confirmasus.seguranca.jwt-segredo=segredo-de-teste-com-pelo-menos-32-caracteres"})
@Import(TestcontainersConfig.class)
class LivaApplicationTests {

	@Autowired
	private DisparoDiarioAgendador agendador;

	@Test
	void contextLoads() {
		assertThat(agendador).isNotNull();
	}

}
