package br.com.conde.compactador.bits;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Lê bit a bit de um fluxo de bytes, na mesma ordem em que o
 * {@link EscritorDeBits} gravou.
 */
public final class LeitorDeBits implements AutoCloseable {

    private final InputStream origem;
    private int acumulador;
    private int restantes;
    private long totalDeBits;

    public LeitorDeBits(InputStream origem) {
        this.origem = origem;
    }

    /** Um leitor sobre um vetor de bytes. */
    public static LeitorDeBits de(byte[] bytes) {
        return new LeitorDeBits(new ByteArrayInputStream(bytes));
    }

    /** Lê um bit, ou -1 quando o fluxo acabou. */
    public int lerOuMenosUm() throws IOException {
        if (restantes == 0) {
            acumulador = origem.read();

            if (acumulador < 0) {
                return -1;
            }

            restantes = 8;
        }

        restantes--;
        totalDeBits++;
        return (acumulador >>> restantes) & 1;
    }

    /** Lê um bit, ou lança quando o fluxo acabou antes da hora. */
    public int ler() throws IOException {
        var bit = lerOuMenosUm();

        if (bit < 0) {
            throw new EOFException("O fluxo acabou no meio de um código.");
        }

        return bit;
    }

    /** Lê {@code quantos} bits como um inteiro. */
    public int ler(int quantos) throws IOException {
        if (quantos < 0 || quantos > 32) {
            throw new IllegalArgumentException("São de 0 a 32 bits, veio " + quantos);
        }

        var valor = 0;

        for (var i = 0; i < quantos; i++) {
            valor = (valor << 1) | ler();
        }

        return valor;
    }

    /** Lê um byte inteiro. */
    public int lerByte() throws IOException {
        return ler(8);
    }

    /** Descarta o que sobrou do byte corrente. */
    public void alinhar() {
        restantes = 0;
    }

    /** Quantos bits já foram lidos. */
    public long totalDeBits() {
        return totalDeBits;
    }

    @Override
    public void close() throws IOException {
        origem.close();
    }
}
