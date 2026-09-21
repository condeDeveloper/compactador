package br.com.conde.compactador.lz77;

import java.util.Arrays;

/**
 * O índice que torna o LZ77 viável.
 *
 * <p>Procurar a maior repetição comparando cada posição com todas as
 * anteriores custaria o quadrado do tamanho da entrada. A saída é indexar as
 * posições por uma chave de três bytes: para cada ponto do texto, a tabela já
 * diz onde aquele mesmo trio apareceu antes, e só esses candidatos precisam
 * ser comparados de verdade.
 */
public final class JanelaDeslizante {

    private static final int VAZIO = -1;
    private static final int BITS_DA_TABELA = 15;
    private static final int TAMANHO_DA_TABELA = 1 << BITS_DA_TABELA;

    private final byte[] dados;
    private final int janela;
    private final int tentativas;
    private final int[] cabeca = new int[TAMANHO_DA_TABELA];
    private final int[] anterior;

    /** Cria o índice sobre os dados. */
    public JanelaDeslizante(byte[] dados, int janela, int tentativas) {
        this.dados = dados;
        this.janela = janela;
        this.tentativas = tentativas;
        this.anterior = new int[dados.length];

        Arrays.fill(cabeca, VAZIO);
        Arrays.fill(anterior, VAZIO);
    }

    /** Registra a posição no índice. */
    public void indexar(int posicao) {
        if (posicao + Lz77.COMPRIMENTO_MINIMO > dados.length) {
            return;
        }

        var chave = chave(posicao);
        anterior[posicao] = cabeca[chave];
        cabeca[chave] = posicao;
    }

    /** A maior repetição que termina antes de {@code posicao}. */
    public Correspondencia procurar(int posicao) {
        if (posicao + Lz77.COMPRIMENTO_MINIMO > dados.length) {
            return Correspondencia.NENHUMA;
        }

        var limite = Math.max(0, posicao - janela);
        var melhorComprimento = 0;
        var melhorDistancia = 0;
        var restantes = tentativas;

        for (var candidato = cabeca[chave(posicao)];
             candidato != VAZIO && candidato >= limite && restantes-- > 0;
             candidato = anterior[candidato]) {

            var comprimento = comparar(candidato, posicao);

            if (comprimento > melhorComprimento) {
                melhorComprimento = comprimento;
                melhorDistancia = posicao - candidato;

                if (comprimento >= Lz77.COMPRIMENTO_MAXIMO) {
                    break;
                }
            }
        }

        return melhorComprimento >= Lz77.COMPRIMENTO_MINIMO
                ? new Correspondencia(melhorDistancia, melhorComprimento)
                : Correspondencia.NENHUMA;
    }

    private int comparar(int candidato, int posicao) {
        var maximo = Math.min(Lz77.COMPRIMENTO_MAXIMO, dados.length - posicao);
        var comprimento = 0;

        // A comparação pode passar do ponto de partida: copiar "ab" com
        // distância 2 e comprimento 6 gera "ababab", porque na descompressão o
        // byte copiado já está disponível quando o seguinte é lido.
        while (comprimento < maximo && dados[candidato + comprimento] == dados[posicao + comprimento]) {
            comprimento++;
        }

        return comprimento;
    }

    private int chave(int posicao) {
        var a = dados[posicao] & 0xFF;
        var b = dados[posicao + 1] & 0xFF;
        var c = dados[posicao + 2] & 0xFF;

        return ((a << 10) ^ (b << 5) ^ c) & (TAMANHO_DA_TABELA - 1);
    }
}
