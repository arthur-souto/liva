package sus.liva.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelefoneTest {

    @ParameterizedTest
    @ValueSource(strings = {"(11) 98765-4321", "11987654321", "5511987654321", "+55 11 98765-4321"})
    void normalizaCelularEmFormatosAceitos(String bruto) {
        assertThat(Telefone.de(bruto).e164()).isEqualTo("+5511987654321");
    }

    @Test
    void aceitaFixoComDezDigitos() {
        assertThat(Telefone.de("(11) 3333-4444").e164()).isEqualTo("+551133334444");
    }

    @Test
    void apenasDigitosRetornaSemMais() {
        assertThat(Telefone.de("(11) 98765-4321").apenasDigitos()).isEqualTo("5511987654321");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "abc", "119876543", "551198765432100", "4411987654321"})
    void rejeitaFormatosInvalidos(String bruto) {
        assertThatThrownBy(() -> Telefone.de(bruto)).isInstanceOf(IllegalArgumentException.class);
    }
}
