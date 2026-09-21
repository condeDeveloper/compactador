package br.com.conde.compactador.bits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.EOFException;
import java.io.IOException;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BitsTest {

    @Test
    @DisplayName("oito bits viram um byte")
    void oitoBitsViramUmByte() throws IOException {
        var escritor = EscritorDeBits.emMemoria();

        for (var bit : new int[] {1, 0, 1, 0, 0, 0, 0, 1}) {
            escritor.escrever(bit);
        }

        assertThat(escritor.bytes()).containsExactly((byte) 0b10100001);
    }

    @Test
    @DisplayName("o primeiro bit escrito e o mais significativo")
    void ordemDosBits() throws IOException {
        var escritor = EscritorDeBits.emMemoria();
        escritor.escrever(1);
        escritor.alinhar();

        assertThat(escritor.bytes()[0] & 0xFF).isEqualTo(0b10000000);
    }

    @Test
    void escreveUmValorComVariosBits() throws IOException {
        var escritor = EscritorDeBits.emMemoria();
        escritor.escrever(0b101, 3);
        escritor.escrever(0b11, 2);
        escritor.alinhar();

        assertThat(escritor.bytes()[0] & 0xFF).isEqualTo(0b10111000);
    }

    @Test
    @DisplayName("alinhar completa o byte com zeros, senao os ultimos bits se perdem")
    void alinhar() throws IOException {
        var escritor = EscritorDeBits.emMemoria();
        escritor.escrever(1);

        assertThat(escritor.bytes()).isEmpty();

        escritor.alinhar();

        assertThat(escritor.bytes()).hasSize(1);
    }

    @Test
    void contaBitsEBytes() throws IOException {
        var escritor = EscritorDeBits.emMemoria();
        escritor.escrever(0, 9);

        assertThat(escritor.totalDeBits()).isEqualTo(9);
        assertThat(escritor.totalDeBytes()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 33})
    void quantidadeDeBitsInvalidaReclama(int quantos) {
        var escritor = EscritorDeBits.emMemoria();

        assertThatThrownBy(() -> escritor.escrever(1, quantos))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a ida e volta preserva a sequencia de bits")
    void idaEVolta() throws IOException {
        var sorteio = new Random(42);
        var bits = new int[1000];

        var escritor = EscritorDeBits.emMemoria();
        for (var i = 0; i < bits.length; i++) {
            bits[i] = sorteio.nextInt(2);
            escritor.escrever(bits[i]);
        }
        escritor.alinhar();

        try (var leitor = LeitorDeBits.de(escritor.bytes())) {
            for (var esperado : bits) {
                assertThat(leitor.ler()).isEqualTo(esperado);
            }
        }
    }

    @Test
    void leValoresComVariosBits() throws IOException {
        var escritor = EscritorDeBits.emMemoria();
        escritor.escrever(1234, 16);
        escritor.escrever(7, 4);
        escritor.alinhar();

        try (var leitor = LeitorDeBits.de(escritor.bytes())) {
            assertThat(leitor.ler(16)).isEqualTo(1234);
            assertThat(leitor.ler(4)).isEqualTo(7);
        }
    }

    @Test
    void oFimDoFluxoDevolveMenosUm() throws IOException {
        try (var leitor = LeitorDeBits.de(new byte[0])) {
            assertThat(leitor.lerOuMenosUm()).isEqualTo(-1);
        }
    }

    @Test
    @DisplayName("ler alem do fim reclama em vez de devolver lixo")
    void lerAlemDoFimReclama() throws IOException {
        try (var leitor = LeitorDeBits.de(new byte[0])) {
            assertThatThrownBy(leitor::ler).isInstanceOf(EOFException.class);
        }
    }

    @Test
    void alinharDescartaORestoDoByte() throws IOException {
        var escritor = EscritorDeBits.emMemoria();
        escritor.escrever(0b1010, 4);
        escritor.alinhar();
        escritor.escreverByte(0xFF);

        try (var leitor = LeitorDeBits.de(escritor.bytes())) {
            assertThat(leitor.ler(4)).isEqualTo(0b1010);
            leitor.alinhar();
            assertThat(leitor.lerByte()).isEqualTo(0xFF);
        }
    }

    @Test
    void contaOsBitsLidos() throws IOException {
        var escritor = EscritorDeBits.emMemoria();
        escritor.escreverByte(0xAB);

        try (var leitor = LeitorDeBits.de(escritor.bytes())) {
            leitor.ler(5);
            assertThat(leitor.totalDeBits()).isEqualTo(5);
        }
    }
}
