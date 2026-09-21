package br.com.conde.compactador.huffman;

import java.util.PriorityQueue;

/**
 * Monta a árvore de Huffman e devolve o comprimento do código de cada símbolo.
 *
 * <p>A árvore em si não precisa sobreviver: o que interessa é a profundidade
 * de cada folha, porque a partir dos comprimentos o código canônico se
 * reconstrói sozinho. Guardar 256 comprimentos é muito mais barato que guardar
 * uma árvore.
 */
public final class ArvoreDeHuffman {

    /**
     * Teto do comprimento de um código.
     *
     * <p>Sem teto, uma distribuição em forma de Fibonacci geraria códigos com
     * centenas de bits, que não caberiam em um inteiro. Quinze bits é o mesmo
     * limite que o deflate usa.
     */
    public static final int LIMITE = 15;

    private record No(int simbolo, long frequencia, No esquerda, No direita) implements Comparable<No> {

        static No folha(int simbolo, long frequencia) {
            return new No(simbolo, frequencia, null, null);
        }

        static No juntar(No esquerda, No direita) {
            return new No(-1, esquerda.frequencia + direita.frequencia, esquerda, direita);
        }

        boolean ehFolha() {
            return esquerda == null && direita == null;
        }

        @Override
        public int compareTo(No outro) {
            var porFrequencia = Long.compare(frequencia, outro.frequencia);
            // O desempate pelo símbolo mantém a árvore igual entre execuções,
            // o que torna a compressão reproduzível.
            return porFrequencia != 0 ? porFrequencia : Integer.compare(simbolo, outro.simbolo);
        }
    }

    private ArvoreDeHuffman() {
    }

    /** Os comprimentos de código, com o teto padrão. */
    public static int[] comprimentos(Frequencias frequencias) {
        return comprimentos(frequencias.contagens(), LIMITE);
    }

    /**
     * Os comprimentos de código respeitando um teto.
     *
     * <p>Quando a árvore passa do teto, as frequências são achatadas pela
     * metade e a árvore é remontada. O código resultante é um pouco pior que o
     * ótimo, mas continua sendo um código de prefixo válido — e cabe.
     */
    public static int[] comprimentos(long[] frequencias, int limite) {
        var atuais = frequencias.clone();

        while (true) {
            var comprimentos = calcular(atuais);

            if (maior(comprimentos) <= limite) {
                return comprimentos;
            }

            achatar(atuais);
        }
    }

    private static int[] calcular(long[] frequencias) {
        var comprimentos = new int[Frequencias.SIMBOLOS];
        var fila = new PriorityQueue<No>();

        for (var simbolo = 0; simbolo < frequencias.length; simbolo++) {
            if (frequencias[simbolo] > 0) {
                fila.add(No.folha(simbolo, frequencias[simbolo]));
            }
        }

        if (fila.isEmpty()) {
            return comprimentos;
        }

        if (fila.size() == 1) {
            // Um símbolo só daria profundidade zero, e um código de zero bits
            // não distingue nada. Um bit é o mínimo que funciona.
            comprimentos[fila.poll().simbolo()] = 1;
            return comprimentos;
        }

        while (fila.size() > 1) {
            fila.add(No.juntar(fila.poll(), fila.poll()));
        }

        medir(fila.poll(), 0, comprimentos);
        return comprimentos;
    }

    private static void medir(No no, int profundidade, int[] comprimentos) {
        if (no.ehFolha()) {
            comprimentos[no.simbolo()] = profundidade;
            return;
        }

        medir(no.esquerda(), profundidade + 1, comprimentos);
        medir(no.direita(), profundidade + 1, comprimentos);
    }

    private static void achatar(long[] frequencias) {
        for (var i = 0; i < frequencias.length; i++) {
            if (frequencias[i] > 0) {
                frequencias[i] = (frequencias[i] + 1) / 2;
            }
        }
    }

    private static int maior(int[] comprimentos) {
        var maior = 0;

        for (var comprimento : comprimentos) {
            maior = Math.max(maior, comprimento);
        }

        return maior;
    }
}
