package br.com.conde.compactador.lz77;

import br.com.conde.compactador.bits.EscritorDeBits;
import br.com.conde.compactador.bits.LeitorDeBits;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Compressão por referência a trecho anterior.
 *
 * <p>Onde o Huffman troca bytes frequentes por códigos curtos, o LZ77 troca
 * trechos repetidos por um par "volte tanto, copie tanto". É o que faz um HTML
 * cheio de tags iguais encolher muito, e é também por que ele não ajuda quase
 * nada em dado já comprimido ou aleatório: não há o que repetir.
 */
public final class Lz77 {

    /** Menor repetição que compensa: menos que isso, o literal sai mais barato. */
    public static final int COMPRIMENTO_MINIMO = 3;

    /** Maior repetição que cabe nos bits reservados ao comprimento. */
    public static final int COMPRIMENTO_MAXIMO = COMPRIMENTO_MINIMO + 255;

    /** Quantos bytes para trás a busca enxerga. */
    public static final int JANELA = 32_768;

    private static final int BITS_DA_DISTANCIA = 15;
    private static final int BITS_DO_COMPRIMENTO = 8;

    /** Quantos candidatos são comparados por posição. */
    public static final int TENTATIVAS = 64;

    private Lz77() {
    }

    /** Comprime. */
    public static byte[] comprimir(byte[] dados) throws IOException {
        return comprimir(dados, JANELA, TENTATIVAS);
    }

    /** Comprime com janela e esforço de busca escolhidos. */
    public static byte[] comprimir(byte[] dados, int janela, int tentativas) throws IOException {
        var saida = new ByteArrayOutputStream();

        try (var escritor = new EscritorDeBits(saida)) {
            escritor.escrever(dados.length, 32);

            var indice = new JanelaDeslizante(dados, janela, tentativas);
            var posicao = 0;

            while (posicao < dados.length) {
                var achado = indice.procurar(posicao);

                if (achado.vale()) {
                    escritor.escrever(1);
                    escritor.escrever(achado.distancia(), BITS_DA_DISTANCIA);
                    escritor.escrever(achado.comprimento() - COMPRIMENTO_MINIMO, BITS_DO_COMPRIMENTO);

                    for (var i = 0; i < achado.comprimento(); i++) {
                        indice.indexar(posicao + i);
                    }

                    posicao += achado.comprimento();
                } else {
                    escritor.escrever(0);
                    escritor.escreverByte(dados[posicao]);
                    indice.indexar(posicao);
                    posicao++;
                }
            }

            escritor.alinhar();
            return saida.toByteArray();
        }
    }

    /** Descomprime o que {@link #comprimir} produziu. */
    public static byte[] descomprimir(byte[] comprimido) throws IOException {
        try (var leitor = LeitorDeBits.de(comprimido)) {
            var quantidade = leitor.ler(32);

            if (quantidade < 0) {
                throw new IOException("Cabeçalho inválido: tamanho negativo.");
            }

            var dados = new byte[quantidade];
            var posicao = 0;

            while (posicao < quantidade) {
                if (leitor.ler() == 0) {
                    dados[posicao++] = (byte) leitor.lerByte();
                    continue;
                }

                var distancia = leitor.ler(BITS_DA_DISTANCIA);
                var comprimento = leitor.ler(BITS_DO_COMPRIMENTO) + COMPRIMENTO_MINIMO;

                if (distancia <= 0 || distancia > posicao) {
                    throw new IOException("Referência para fora do já descomprimido: " + distancia);
                }

                if (posicao + comprimento > quantidade) {
                    throw new IOException("Referência passa do fim do arquivo.");
                }

                // A cópia é byte a byte de propósito: quando o comprimento
                // passa da distância, os bytes recém-escritos alimentam a
                // própria cópia, e é assim que <1,n> repete um byte n vezes.
                for (var i = 0; i < comprimento; i++) {
                    dados[posicao] = dados[posicao - distancia];
                    posicao++;
                }
            }

            return dados;
        }
    }
}
