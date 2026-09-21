package br.com.conde.compactador.huffman;

/**
 * Quantas vezes cada byte aparece na entrada.
 *
 * <p>É o único dado de que o Huffman precisa: quem aparece mais ganha código
 * curto, quem aparece menos ganha código longo.
 */
public final class Frequencias {

    /** Quantos símbolos diferentes um byte pode assumir. */
    public static final int SIMBOLOS = 256;

    private final long[] contagem = new long[SIMBOLOS];

    /** Conta os bytes de uma entrada. */
    public static Frequencias de(byte[] dados) {
        var frequencias = new Frequencias();

        for (var b : dados) {
            frequencias.contagem[b & 0xFF]++;
        }

        return frequencias;
    }

    /** Quantas vezes o símbolo aparece. */
    public long de(int simbolo) {
        return contagem[simbolo];
    }

    /** O vetor de contagens. */
    public long[] contagens() {
        return contagem.clone();
    }

    /** Quantos símbolos diferentes aparecem ao menos uma vez. */
    public int distintos() {
        var quantos = 0;

        for (var frequencia : contagem) {
            if (frequencia > 0) {
                quantos++;
            }
        }

        return quantos;
    }

    /** O total de bytes contados. */
    public long total() {
        var soma = 0L;

        for (var frequencia : contagem) {
            soma += frequencia;
        }

        return soma;
    }

    /**
     * A entropia de Shannon, em bits por símbolo.
     *
     * <p>É o piso teórico: nenhum código que trate os bytes um a um consegue
     * ficar abaixo disso. Serve para saber quanto ainda dá para espremer — e
     * para entender por que texto comprime e ruído não.
     */
    public double entropia() {
        var total = total();

        if (total == 0) {
            return 0;
        }

        var soma = 0.0;

        for (var frequencia : contagem) {
            if (frequencia > 0) {
                var probabilidade = (double) frequencia / total;
                soma -= probabilidade * (Math.log(probabilidade) / Math.log(2));
            }
        }

        return soma;
    }
}
