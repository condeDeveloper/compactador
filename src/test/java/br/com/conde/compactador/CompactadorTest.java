package br.com.conde.compactador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CompactadorTest {

    private static byte[] texto(String conteudo) {
        return conteudo.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] aleatorio(int tamanho, long semente) {
        var dados = new byte[tamanho];
        new Random(semente).nextBytes(dados);
        return dados;
    }

    @ParameterizedTest
    @EnumSource(Metodo.class)
    @DisplayName("todo método sobrevive à ida e volta")
    void idaEVoltaPorMetodo(Metodo metodo) throws IOException {
        var dados = texto("o rato roeu a roupa do rei de roma ".repeat(20));

        var comprimido = Compactador.comprimirCom(dados, metodo);

        assertThat(Compactador.descomprimir(comprimido)).isEqualTo(dados);
    }

    @Test
    @DisplayName("texto desigual sem repetição vai de Huffman")
    void textoDesigualVaiDeHuffman() throws IOException {
        var dados = new byte[6000];
        var sorteio = new Random(5);

        // Alfabeto de quatro letras com pesos bem diferentes e sem estrutura:
        // muita desigualdade de frequência, pouca repetição de trecho.
        for (var i = 0; i < dados.length; i++) {
            var sorte = sorteio.nextInt(100);
            dados[i] = (byte) (sorte < 70 ? 'a' : sorte < 90 ? 'b' : sorte < 97 ? 'c' : 'd');
        }

        assertThat(Compactador.escolher(dados)).isEqualTo(Metodo.HUFFMAN);
    }

    @Test
    @DisplayName("texto repetitivo vai de LZ77")
    void textoRepetitivoVaiDeLz77() throws IOException {
        var dados = texto("<tr><td>valor</td></tr>".repeat(300));

        assertThat(Compactador.escolher(dados)).isEqualTo(Metodo.LZ77);
    }

    @Test
    @DisplayName("dado aleatório fica armazenado: comprimir só pioraria")
    void dadoAleatorioFicaArmazenado() throws IOException {
        var dados = aleatorio(4000, 13);

        assertThat(Compactador.escolher(dados)).isEqualTo(Metodo.ARMAZENADO);
    }

    @Test
    @DisplayName("no pior caso a saída cresce três bytes, não o dobro")
    void noPiorCasoCresceTresBytes() throws IOException {
        var dados = aleatorio(4000, 17);

        var comprimido = Compactador.comprimir(dados);

        assertThat(comprimido.length).isEqualTo(dados.length + 3);
        assertThat(Compactador.descomprimir(comprimido)).isEqualTo(dados);
    }

    @Test
    void entradaVaziaFicaArmazenada() throws IOException {
        assertThat(Compactador.escolher(new byte[0])).isEqualTo(Metodo.ARMAZENADO);
        assertThat(Compactador.descomprimir(Compactador.comprimir(new byte[0]))).isEmpty();
    }

    @Test
    @DisplayName("mil zeros encolhem muito")
    void milZerosEncolhem() throws IOException {
        var dados = new byte[1000];

        var comprimido = Compactador.comprimir(dados);

        assertThat(comprimido.length).isLessThan(60);
        assertThat(Compactador.descomprimir(comprimido)).isEqualTo(dados);
    }

    @Test
    void arquivoSemAssinaturaReclama() {
        assertThatThrownBy(() -> Compactador.descomprimir(new byte[] {1, 2, 3, 4}))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Assinatura");
    }

    @Test
    void arquivoCurtoDemaisReclama() {
        assertThatThrownBy(() -> Compactador.descomprimir(new byte[] {1}))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("curto");
    }

    @Test
    void metodoDesconhecidoReclama() {
        var comprimido = new byte[] {(byte) 0xC0, (byte) 0xDE, 99, 0, 0, 0, 0};

        assertThatThrownBy(() -> Compactador.descomprimir(comprimido))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Método desconhecido");
    }

    @Test
    void osCodigosDoCabecalhoSaoEstaveis() {
        assertThat(Metodo.de(0)).isEqualTo(Metodo.ARMAZENADO);
        assertThat(Metodo.de(1)).isEqualTo(Metodo.HUFFMAN);
        assertThat(Metodo.de(2)).isEqualTo(Metodo.LZ77);
        assertThatThrownBy(() -> Metodo.de(7)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("as estatísticas contam o que foi economizado")
    void estatisticas() throws IOException {
        var dados = texto("a".repeat(10_000));

        var medida = Compactador.medir(dados);

        assertThat(medida.original()).isEqualTo(10_000);
        assertThat(medida.encolheu()).isTrue();
        assertThat(medida.economia()).isGreaterThan(90);
        assertThat(medida.bitsPorByte()).isLessThan(1);
        assertThat(medida.toString()).contains("menor");
    }

    @Test
    void estatisticaDeEntradaVazia() {
        var medida = new Estatisticas(Metodo.ARMAZENADO, 0, 3);

        assertThat(medida.razao()).isEqualTo(1);
        assertThat(medida.bitsPorByte()).isZero();
    }

    @Test
    @DisplayName("a entropia é o piso: dado aleatório fica em oito bits por byte")
    void entropiaDeDadoAleatorio() {
        assertThat(Compactador.entropia(aleatorio(50_000, 23))).isCloseTo(8.0, within(0.02));
    }

    @Test
    @DisplayName("texto de verdade fica bem abaixo de oito bits por byte")
    void entropiaDeTexto() {
        var dados = texto("A compressao funciona porque a linguagem escrita e redundante. ".repeat(50));

        assertThat(Compactador.entropia(dados)).isLessThan(5.0);
    }

    @Test
    @DisplayName("o método escolhido nunca é pior que os outros")
    void oEscolhidoEOMenor() throws IOException {
        var entradas = new byte[][] {
            texto("abcabcabc".repeat(100)),
            texto("a".repeat(5000)),
            aleatorio(2000, 29),
            texto("texto comum de tamanho moderado para variar um pouco o teste"),
        };

        for (var dados : entradas) {
            var escolhido = Compactador.comprimir(dados).length;

            for (var metodo : Metodo.values()) {
                assertThat(escolhido).isLessThanOrEqualTo(Compactador.comprimirCom(dados, metodo).length);
            }
        }
    }
}
