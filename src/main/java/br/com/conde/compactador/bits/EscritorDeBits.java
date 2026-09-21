package br.com.conde.compactador.bits;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Escreve bit a bit em um fluxo de bytes.
 *
 * <p>Compressão só faz sentido abaixo do byte: um código de Huffman pode ter
 * três bits, e gravar cada um em um byte jogaria fora justamente o que se
 * economizou. Os bits vão se acumulando em um buffer de oito e só então viram
 * byte, do mais significativo para o menos.
 */
public final class EscritorDeBits implements AutoCloseable {

    private final OutputStream destino;
    private int acumulador;
    private int preenchidos;
    private long totalDeBits;

    public EscritorDeBits(OutputStream destino) {
        this.destino = destino;
    }

    /** Um escritor que guarda tudo em memória. */
    public static EscritorDeBits emMemoria() {
        return new EscritorDeBits(new ByteArrayOutputStream());
    }

    /** Escreve um bit. Qualquer valor diferente de zero conta como 1. */
    public void escrever(int bit) throws IOException {
        acumulador = (acumulador << 1) | (bit != 0 ? 1 : 0);
        preenchidos++;
        totalDeBits++;

        if (preenchidos == 8) {
            destino.write(acumulador);
            acumulador = 0;
            preenchidos = 0;
        }
    }

    /** Escreve os {@code quantos} bits menos significativos, do maior para o menor. */
    public void escrever(int valor, int quantos) throws IOException {
        if (quantos < 0 || quantos > 32) {
            throw new IllegalArgumentException("São de 0 a 32 bits, veio " + quantos);
        }

        for (var i = quantos - 1; i >= 0; i--) {
            escrever((valor >>> i) & 1);
        }
    }

    /** Escreve um byte inteiro. */
    public void escreverByte(int valor) throws IOException {
        escrever(valor & 0xFF, 8);
    }

    /**
     * Completa o byte em aberto com zeros. Sem isso os últimos bits ficariam
     * dentro do acumulador e nunca chegariam ao fluxo.
     */
    public void alinhar() throws IOException {
        while (preenchidos != 0) {
            escrever(0);
        }
    }

    /** Quantos bits já foram escritos. */
    public long totalDeBits() {
        return totalDeBits;
    }

    /** Quantos bytes o resultado ocupa, contando o byte em aberto. */
    public long totalDeBytes() {
        return (totalDeBits + 7) / 8;
    }

    /** O conteúdo, quando o destino é memória. */
    public byte[] bytes() {
        if (destino instanceof ByteArrayOutputStream memoria) {
            return memoria.toByteArray();
        }
        throw new IllegalStateException("O destino não é memória.");
    }

    @Override
    public void close() throws IOException {
        alinhar();
        destino.flush();
    }
}
