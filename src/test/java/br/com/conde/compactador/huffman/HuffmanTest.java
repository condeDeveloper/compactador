package br.com.conde.compactador.huffman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import br.com.conde.compactador.bits.EscritorDeBits;
import br.com.conde.compactador.bits.LeitorDeBits;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HuffmanTest {

    private static final int A = 'a';
    private static final int B = 'b';
    private static final int C = 'c';
    private static final int D = 'd';
    private static final int Z = 'z';

    private static byte[] texto(String conteudo) {
        return conteudo.getBytes(StandardCharsets.UTF_8);
    }

    private static int maiorDe(int[] comprimentos) {
        var maior = 0;

        for (var comprimento : comprimentos) {
            maior = Math.max(maior, comprimento);
        }

        return maior;
    }

    private static long[] fibonacci(int quantos) {
        var frequencias = new long[Frequencias.SIMBOLOS];
        long anterior = 1;
        long atual = 1;

        for (var simbolo = 0; simbolo < quantos; simbolo++) {
            frequencias[simbolo] = atual;
            var proximo = anterior + atual;
            anterior = atual;
            atual = proximo;
        }

        return frequencias;
    }

    @Nested
    @DisplayName("frequências")
    class Contagem {

        @Test
        void contaCadaByte() {
            var frequencias = Frequencias.de(texto("aab"));

            assertThat(frequencias.de(A)).isEqualTo(2);
            assertThat(frequencias.de(B)).isEqualTo(1);
            assertThat(frequencias.de(Z)).isZero();
            assertThat(frequencias.total()).isEqualTo(3);
            assertThat(frequencias.distintos()).isEqualTo(2);
        }

        @Test
        @DisplayName("a entropia de um símbolo só é zero: não há o que informar")
        void entropiaDeUmSimboloSo() {
            assertThat(Frequencias.de(texto("aaaa")).entropia()).isEqualTo(0);
        }

        @Test
        @DisplayName("dois símbolos igualmente prováveis custam um bit cada")
        void entropiaDeDoisSimbolos() {
            assertThat(Frequencias.de(texto("abab")).entropia()).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("256 símbolos uniformes custam oito bits: não há compressão possível")
        void entropiaMaxima() {
            var dados = new byte[256];
            for (var i = 0; i < 256; i++) {
                dados[i] = (byte) i;
            }

            assertThat(Frequencias.de(dados).entropia()).isCloseTo(8.0, within(1e-9));
        }

        @Test
        void entropiaDeNadaEZero() {
            assertThat(Frequencias.de(new byte[0]).entropia()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("comprimentos de código")
    class Comprimentos {

        @Test
        @DisplayName("quem aparece mais recebe código mais curto")
        void oMaisFrequenteRecebeOMenor() {
            var comprimentos = ArvoreDeHuffman.comprimentos(Frequencias.de(texto("aaaaaaaabbbbccd")));

            assertThat(comprimentos[A]).isLessThan(comprimentos[B]);
            assertThat(comprimentos[B]).isLessThanOrEqualTo(comprimentos[C]);
            assertThat(comprimentos[C]).isLessThanOrEqualTo(comprimentos[D]);
        }

        @Test
        @DisplayName("um símbolo só ainda precisa de um bit")
        void umSimboloSo() {
            assertThat(ArvoreDeHuffman.comprimentos(Frequencias.de(texto("aaaa")))[A]).isEqualTo(1);
        }

        @Test
        void entradaVaziaNaoGeraCodigo() {
            assertThat(ArvoreDeHuffman.comprimentos(Frequencias.de(new byte[0]))).containsOnly(0);
        }

        @Test
        @DisplayName("a desigualdade de Kraft vale: o código é um prefixo válido")
        void desigualdadeDeKraft() {
            var comprimentos = ArvoreDeHuffman.comprimentos(Frequencias.de(texto("abracadabra alakazam")));

            var soma = 0.0;
            for (var comprimento : comprimentos) {
                if (comprimento > 0) {
                    soma += Math.pow(2, -comprimento);
                }
            }

            assertThat(soma).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("distribuição de Fibonacci geraria códigos enormes; o teto segura")
        void oTetoSeguraOComprimento() {
            var comprimentos = ArvoreDeHuffman.comprimentos(fibonacci(40), ArvoreDeHuffman.LIMITE);

            assertThat(maiorDe(comprimentos)).isLessThanOrEqualTo(ArvoreDeHuffman.LIMITE);
        }

        @Test
        @DisplayName("sem teto a mesma distribuição passa de quinze bits")
        void semTetoOsCodigosCrescem() {
            var comprimentos = ArvoreDeHuffman.comprimentos(fibonacci(40), 64);

            assertThat(maiorDe(comprimentos)).isGreaterThan(ArvoreDeHuffman.LIMITE);
        }
    }

    @Nested
    @DisplayName("código canônico")
    class Canonico {

        @Test
        @DisplayName("o decodificador remonta o código só com os comprimentos")
        void remontaPelosComprimentos() throws IOException {
            var original = CodigoCanonico.de(ArvoreDeHuffman.comprimentos(Frequencias.de(texto("abracadabra"))));

            var escritor = EscritorDeBits.emMemoria();
            original.escreverTabela(escritor);
            escritor.alinhar();

            var remontado = CodigoCanonico.lerTabela(LeitorDeBits.de(escritor.bytes()));

            for (var simbolo = 0; simbolo < Frequencias.SIMBOLOS; simbolo++) {
                assertThat(remontado.comprimentoDe(simbolo)).isEqualTo(original.comprimentoDe(simbolo));
                assertThat(remontado.codigoDe(simbolo)).isEqualTo(original.codigoDe(simbolo));
            }
        }

        @Test
        @DisplayName("símbolos de mesmo comprimento recebem códigos consecutivos")
        void codigosConsecutivos() {
            var comprimentos = new int[Frequencias.SIMBOLOS];
            comprimentos[A] = 2;
            comprimentos[B] = 2;
            comprimentos[C] = 2;
            comprimentos[D] = 2;

            var codigo = CodigoCanonico.de(comprimentos);

            assertThat(codigo.codigoDe(B)).isEqualTo(codigo.codigoDe(A) + 1);
            assertThat(codigo.codigoDe(C)).isEqualTo(codigo.codigoDe(B) + 1);
            assertThat(codigo.simbolos()).isEqualTo(4);
            assertThat(codigo.maiorComprimento()).isEqualTo(2);
        }

        @Test
        @DisplayName("escrever e ler um símbolo devolve o mesmo símbolo")
        void escreveELe() throws IOException {
            var codigo = CodigoCanonico.de(ArvoreDeHuffman.comprimentos(Frequencias.de(texto("abracadabra"))));

            var escritor = EscritorDeBits.emMemoria();
            codigo.escrever(escritor, A);
            codigo.escrever(escritor, B);
            escritor.alinhar();

            try (var leitor = LeitorDeBits.de(escritor.bytes())) {
                assertThat(codigo.ler(leitor)).isEqualTo(A);
                assertThat(codigo.ler(leitor)).isEqualTo(B);
            }
        }

        @Test
        void tabelaDeTamanhoErradoReclama() {
            assertThatThrownBy(() -> CodigoCanonico.de(new int[10]))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void simboloForaDoCodigoReclama() {
            var codigo = CodigoCanonico.de(ArvoreDeHuffman.comprimentos(Frequencias.de(texto("aaa"))));

            assertThatThrownBy(() -> codigo.escrever(EscritorDeBits.emMemoria(), Z))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("ida e volta")
    class IdaEVolta {

        @Test
        void textoComum() throws IOException {
            var dados = texto("a compressão só existe porque a informação é desigual");

            assertThat(Huffman.descomprimir(Huffman.comprimir(dados))).isEqualTo(dados);
        }

        @Test
        void entradaVazia() throws IOException {
            assertThat(Huffman.descomprimir(Huffman.comprimir(new byte[0]))).isEmpty();
        }

        @Test
        void umByteSo() throws IOException {
            var dados = new byte[] {42};

            assertThat(Huffman.descomprimir(Huffman.comprimir(dados))).isEqualTo(dados);
        }

        @Test
        void umSimboloRepetido() throws IOException {
            var dados = new byte[1000];

            assertThat(Huffman.descomprimir(Huffman.comprimir(dados))).isEqualTo(dados);
        }

        @Test
        void todosOsBytesPossiveis() throws IOException {
            var dados = new byte[256];
            for (var i = 0; i < 256; i++) {
                dados[i] = (byte) i;
            }

            assertThat(Huffman.descomprimir(Huffman.comprimir(dados))).isEqualTo(dados);
        }

        @Test
        void dadosAleatorios() throws IOException {
            var dados = new byte[5000];
            new Random(7).nextBytes(dados);

            assertThat(Huffman.descomprimir(Huffman.comprimir(dados))).isEqualTo(dados);
        }
    }

    @Test
    @DisplayName("texto muito desigual comprime bem")
    void textoDesigualComprime() throws IOException {
        var dados = texto("a".repeat(9000) + "b".repeat(1000));

        assertThat(Huffman.comprimir(dados).length).isLessThan(dados.length / 2);
    }

    @Test
    @DisplayName("a estimativa bate com o tamanho real")
    void estimativaBateComOReal() throws IOException {
        var dados = texto("abracadabra ".repeat(100));

        assertThat(Huffman.tamanhoEstimado(dados)).isEqualTo(Huffman.comprimir(dados).length);
    }
}
