package br.com.conde.compactador.lz77;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Lz77Test {

    private static byte[] texto(String conteudo) {
        return conteudo.getBytes(StandardCharsets.UTF_8);
    }

    private static String devolta(byte[] dados) throws IOException {
        return new String(Lz77.descomprimir(Lz77.comprimir(dados)), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("a correspondência vazia não vale a pena")
    void correspondenciaVazia() {
        assertThat(Correspondencia.NENHUMA.vale()).isFalse();
        assertThat(new Correspondencia(10, 2).vale()).isFalse();
        assertThat(new Correspondencia(10, 3).vale()).isTrue();
    }

    @Test
    void correspondenciaNegativaReclama() {
        assertThatThrownBy(() -> new Correspondencia(-1, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCorrespondenciaSeDescreveDeVolta() {
        assertThat(new Correspondencia(4, 6)).hasToString("<4,6>");
        assertThat(Correspondencia.NENHUMA).hasToString("-");
    }

    @Test
    @DisplayName("a janela encontra a repetição mais longa")
    void aJanelaEncontraARepeticao() {
        var dados = texto("abcabcabc");
        var janela = new JanelaDeslizante(dados, Lz77.JANELA, Lz77.TENTATIVAS);

        for (var i = 0; i < 3; i++) {
            janela.indexar(i);
        }

        var achado = janela.procurar(3);

        assertThat(achado.distancia()).isEqualTo(3);
        assertThat(achado.comprimento()).isEqualTo(6);
    }

    @Test
    @DisplayName("sem repetição não há correspondência")
    void semRepeticao() {
        var dados = texto("abcdef");
        var janela = new JanelaDeslizante(dados, Lz77.JANELA, Lz77.TENTATIVAS);

        for (var i = 0; i < 3; i++) {
            janela.indexar(i);
        }

        assertThat(janela.procurar(3).vale()).isFalse();
    }

    @Test
    @DisplayName("perto do fim não sobra espaço para o mínimo de três bytes")
    void pertoDoFim() {
        var dados = texto("abcab");
        var janela = new JanelaDeslizante(dados, Lz77.JANELA, Lz77.TENTATIVAS);

        for (var i = 0; i < 3; i++) {
            janela.indexar(i);
        }

        assertThat(janela.procurar(4).vale()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a",
        "ab",
        "abc",
        "aaaaaaaaaaaaaaaaaaaa",
        "abcabcabcabcabcabcabc",
        "o rato roeu a roupa do rei de roma",
        "0123456789",
    })
    @DisplayName("a ida e volta devolve o original")
    void idaEVolta(String conteudo) throws IOException {
        assertThat(devolta(texto(conteudo))).isEqualTo(conteudo);
    }

    @Test
    @DisplayName("um byte repetido vira uma referência que se alimenta")
    void referenciaQueSeAlimenta() throws IOException {
        // <1,n> copia o byte anterior n vezes: cada byte copiado já está
        // disponível para o seguinte. É o truque que faz mil zeros caberem em
        // poucos bytes.
        var dados = new byte[1000];

        var comprimido = Lz77.comprimir(dados);

        assertThat(comprimido.length).isLessThan(50);
        assertThat(Lz77.descomprimir(comprimido)).isEqualTo(dados);
    }

    @Test
    @DisplayName("texto repetitivo encolhe muito")
    void textoRepetitivoEncolhe() throws IOException {
        var dados = texto("<div class=\"linha\"></div>".repeat(200));

        assertThat(Lz77.comprimir(dados).length).isLessThan(dados.length / 10);
    }

    @Test
    @DisplayName("dado aleatório não comprime: não há o que repetir")
    void dadoAleatorioNaoComprime() throws IOException {
        var dados = new byte[4000];
        new Random(11).nextBytes(dados);

        var comprimido = Lz77.comprimir(dados);

        assertThat(comprimido.length).isGreaterThan(dados.length);
        assertThat(Lz77.descomprimir(comprimido)).isEqualTo(dados);
    }

    @Test
    void dadosAleatoriosVoltamIguais() throws IOException {
        var dados = new byte[20_000];
        new Random(3).nextBytes(dados);

        assertThat(Lz77.descomprimir(Lz77.comprimir(dados))).isEqualTo(dados);
    }

    @Test
    @DisplayName("a janela menor acha menos repetição")
    void aJanelaMenorAchaMenos() throws IOException {
        var dados = texto("abcdefghij".repeat(100));

        var comJanelaGrande = Lz77.comprimir(dados, Lz77.JANELA, Lz77.TENTATIVAS).length;
        var comJanelaMinima = Lz77.comprimir(dados, 4, Lz77.TENTATIVAS).length;

        assertThat(comJanelaGrande).isLessThan(comJanelaMinima);
    }

    @Test
    @DisplayName("referência para trás demais é recusada em vez de corromper")
    void referenciaInvalidaReclama() throws IOException {
        var comprimido = Lz77.comprimir(texto("abcabcabc"));

        // Estraga o primeiro token: marca como referência logo no começo, onde
        // ainda não há nada para trás.
        comprimido[4] = (byte) 0xFF;

        assertThatThrownBy(() -> Lz77.descomprimir(comprimido)).isInstanceOf(IOException.class);
    }

    @Test
    void tamanhoNegativoNoCabecalhoReclama() {
        var comprimido = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0};

        assertThatThrownBy(() -> Lz77.descomprimir(comprimido)).isInstanceOf(IOException.class);
    }
}
