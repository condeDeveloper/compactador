package br.com.conde.compactador.huffman;

import br.com.conde.compactador.bits.EscritorDeBits;
import br.com.conde.compactador.bits.LeitorDeBits;
import java.io.IOException;

/**
 * O código de Huffman em forma canônica.
 *
 * <p>Duas árvores com os mesmos comprimentos de código comprimem igual, então
 * não faz sentido transmitir a árvore: basta combinar uma regra de atribuição.
 * A regra é ordenar os símbolos por comprimento e, dentro do mesmo
 * comprimento, por valor — e ir contando em binário. Assim o decodificador
 * reconstrói tudo a partir de 256 comprimentos, sem receber um único ponteiro.
 */
public final class CodigoCanonico {

    private final int[] comprimentos;
    private final int[] codigos;
    private final int[] quantosPorComprimento;
    private final int[] simbolosOrdenados;
    private final int maiorComprimento;

    private CodigoCanonico(int[] comprimentos) {
        this.comprimentos = comprimentos;
        this.codigos = new int[Frequencias.SIMBOLOS];

        var maior = 0;
        for (var comprimento : comprimentos) {
            maior = Math.max(maior, comprimento);
        }
        this.maiorComprimento = maior;

        this.quantosPorComprimento = new int[maior + 1];
        for (var comprimento : comprimentos) {
            if (comprimento > 0) {
                quantosPorComprimento[comprimento]++;
            }
        }

        this.simbolosOrdenados = new int[contarUsados(comprimentos)];
        atribuir();
    }

    /** Monta o código a partir dos comprimentos. */
    public static CodigoCanonico de(int[] comprimentos) {
        if (comprimentos.length != Frequencias.SIMBOLOS) {
            throw new IllegalArgumentException("São esperados " + Frequencias.SIMBOLOS + " comprimentos.");
        }

        return new CodigoCanonico(comprimentos.clone());
    }

    private static int contarUsados(int[] comprimentos) {
        var quantos = 0;

        for (var comprimento : comprimentos) {
            if (comprimento > 0) {
                quantos++;
            }
        }

        return quantos;
    }

    private void atribuir() {
        var codigo = 0;
        var posicao = 0;

        for (var comprimento = 1; comprimento <= maiorComprimento; comprimento++) {
            for (var simbolo = 0; simbolo < Frequencias.SIMBOLOS; simbolo++) {
                if (comprimentos[simbolo] == comprimento) {
                    codigos[simbolo] = codigo++;
                    simbolosOrdenados[posicao++] = simbolo;
                }
            }

            codigo <<= 1;
        }
    }

    /** O comprimento do código de um símbolo, ou zero se ele não aparece. */
    public int comprimentoDe(int simbolo) {
        return comprimentos[simbolo];
    }

    /** O código de um símbolo, alinhado à direita. */
    public int codigoDe(int simbolo) {
        return codigos[simbolo];
    }

    /** O maior comprimento em uso. */
    public int maiorComprimento() {
        return maiorComprimento;
    }

    /** Quantos símbolos o código cobre. */
    public int simbolos() {
        return simbolosOrdenados.length;
    }

    /**
     * Quantos bits a entrada ocuparia com este código. Serve para decidir se
     * vale a pena comprimir antes de comprimir.
     */
    public long bitsPara(Frequencias frequencias) {
        var total = 0L;

        for (var simbolo = 0; simbolo < Frequencias.SIMBOLOS; simbolo++) {
            total += frequencias.de(simbolo) * comprimentos[simbolo];
        }

        return total;
    }

    /** Escreve o código de um símbolo. */
    public void escrever(EscritorDeBits escritor, int simbolo) throws IOException {
        var comprimento = comprimentos[simbolo];

        if (comprimento == 0) {
            throw new IllegalArgumentException("O símbolo " + simbolo + " não está no código.");
        }

        escritor.escrever(codigos[simbolo], comprimento);
    }

    /**
     * Lê um símbolo.
     *
     * <p>A decodificação anda um bit por vez comparando o valor acumulado com
     * o primeiro código de cada comprimento. Como os códigos são canônicos, a
     * posição dentro do comprimento dá o símbolo direto, sem percorrer árvore
     * nenhuma.
     */
    public int ler(LeitorDeBits leitor) throws IOException {
        var codigo = 0;
        var primeiro = 0;
        var indice = 0;

        for (var comprimento = 1; comprimento <= maiorComprimento; comprimento++) {
            codigo |= leitor.ler();
            var quantos = quantosPorComprimento[comprimento];

            if (codigo - primeiro < quantos) {
                return simbolosOrdenados[indice + (codigo - primeiro)];
            }

            indice += quantos;
            primeiro = (primeiro + quantos) << 1;
            codigo <<= 1;
        }

        throw new IOException("Sequência de bits não corresponde a nenhum código.");
    }

    /** Grava a tabela de comprimentos, que é tudo o que o decodificador precisa. */
    public void escreverTabela(EscritorDeBits escritor) throws IOException {
        for (var simbolo = 0; simbolo < Frequencias.SIMBOLOS; simbolo++) {
            escritor.escreverByte(comprimentos[simbolo]);
        }
    }

    /** Lê a tabela de comprimentos e remonta o código. */
    public static CodigoCanonico lerTabela(LeitorDeBits leitor) throws IOException {
        var comprimentos = new int[Frequencias.SIMBOLOS];

        for (var simbolo = 0; simbolo < Frequencias.SIMBOLOS; simbolo++) {
            comprimentos[simbolo] = leitor.lerByte();
        }

        return de(comprimentos);
    }

    /** Quantos bytes a tabela ocupa. */
    public static int tamanhoDaTabela() {
        return Frequencias.SIMBOLOS;
    }
}
