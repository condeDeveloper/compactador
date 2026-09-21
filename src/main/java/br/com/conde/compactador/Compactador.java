package br.com.conde.compactador;

import br.com.conde.compactador.huffman.Frequencias;
import br.com.conde.compactador.huffman.Huffman;
import br.com.conde.compactador.lz77.Lz77;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * A porta de entrada: tenta os métodos e fica com o menor.
 *
 * <pre>{@code
 * byte[] comprimido = Compactador.comprimir(dados);
 * byte[] devolta = Compactador.descomprimir(comprimido);
 * }</pre>
 *
 * <p>Guardar o método em um byte de cabeçalho é o que permite ter
 * {@link Metodo#ARMAZENADO} como rede de segurança: quando nada comprime — dado
 * aleatório, arquivo já compactado — a saída fica um byte maior que a entrada,
 * e não o dobro.
 */
public final class Compactador {

    /** Assinatura do formato, para recusar arquivo que não é nosso. */
    public static final int ASSINATURA = 0xC0DE;

    private Compactador() {
    }

    /** Comprime pelo melhor método disponível. */
    public static byte[] comprimir(byte[] dados) throws IOException {
        return comprimirCom(dados, escolher(dados));
    }

    /** Comprime forçando um método. */
    public static byte[] comprimirCom(byte[] dados, Metodo metodo) throws IOException {
        var corpo = switch (metodo) {
            case ARMAZENADO -> dados.clone();
            case HUFFMAN -> Huffman.comprimir(dados);
            case LZ77 -> Lz77.comprimir(dados);
        };

        var saida = new ByteArrayOutputStream(corpo.length + 3);
        saida.write(ASSINATURA >>> 8);
        saida.write(ASSINATURA & 0xFF);
        saida.write(metodo.codigo());
        saida.write(corpo, 0, corpo.length);

        return saida.toByteArray();
    }

    /** Descomprime, qualquer que tenha sido o método. */
    public static byte[] descomprimir(byte[] comprimido) throws IOException {
        if (comprimido.length < 3) {
            throw new IOException("Arquivo curto demais para ter cabeçalho.");
        }

        var assinatura = ((comprimido[0] & 0xFF) << 8) | (comprimido[1] & 0xFF);

        if (assinatura != ASSINATURA) {
            throw new IOException("Assinatura inválida: não é um arquivo deste compactador.");
        }

        var metodo = metodoDe(comprimido[2] & 0xFF);
        var corpo = Arrays.copyOfRange(comprimido, 3, comprimido.length);

        return switch (metodo) {
            case ARMAZENADO -> corpo;
            case HUFFMAN -> Huffman.descomprimir(corpo);
            case LZ77 -> Lz77.descomprimir(corpo);
        };
    }

    /** O método que sairia menor, sem comprimir de verdade nos casos caros. */
    public static Metodo escolher(byte[] dados) throws IOException {
        if (dados.length == 0) {
            return Metodo.ARMAZENADO;
        }

        var melhor = Metodo.ARMAZENADO;
        var menor = (long) dados.length;

        var porHuffman = Huffman.tamanhoEstimado(dados);
        if (porHuffman < menor) {
            melhor = Metodo.HUFFMAN;
            menor = porHuffman;
        }

        // O LZ77 não tem estimativa barata: descobrir quanto ele comprime é
        // basicamente comprimir. Como ele costuma ganhar em dado repetitivo,
        // vale pagar esse custo.
        var porLz77 = Lz77.comprimir(dados).length;
        if (porLz77 < menor) {
            melhor = Metodo.LZ77;
        }

        return melhor;
    }

    /** Comprime e devolve os números junto com o resultado. */
    public static Estatisticas medir(byte[] dados) throws IOException {
        var metodo = escolher(dados);
        var comprimido = comprimirCom(dados, metodo);

        return new Estatisticas(metodo, dados.length, comprimido.length);
    }

    /** A entropia da entrada, em bits por byte: o piso de qualquer código por símbolo. */
    public static double entropia(byte[] dados) {
        return Frequencias.de(dados).entropia();
    }

    private static Metodo metodoDe(int codigo) throws IOException {
        try {
            return Metodo.de(codigo);
        } catch (IllegalArgumentException erro) {
            throw new IOException(erro.getMessage(), erro);
        }
    }
}
