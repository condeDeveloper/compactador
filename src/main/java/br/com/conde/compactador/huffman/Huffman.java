package br.com.conde.compactador.huffman;

import br.com.conde.compactador.bits.EscritorDeBits;
import br.com.conde.compactador.bits.LeitorDeBits;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Compressão por código de Huffman.
 *
 * <p>Funciona bem quando alguns bytes aparecem muito mais que outros — texto,
 * por exemplo, onde o espaço e as vogais dominam. Não enxerga repetição de
 * trecho: "abcabcabc" tem os três bytes igualmente frequentes e não comprime
 * nada aqui. Para isso existe o LZ77.
 */
public final class Huffman {

    private Huffman() {
    }

    /** Comprime, devolvendo cabeçalho, tabela e dados. */
    public static byte[] comprimir(byte[] dados) throws IOException {
        var saida = new ByteArrayOutputStream();

        try (var escritor = new EscritorDeBits(saida)) {
            escritor.escrever(dados.length, 32);

            if (dados.length == 0) {
                return terminar(escritor, saida);
            }

            var codigo = CodigoCanonico.de(ArvoreDeHuffman.comprimentos(Frequencias.de(dados)));
            codigo.escreverTabela(escritor);

            for (var b : dados) {
                codigo.escrever(escritor, b & 0xFF);
            }

            return terminar(escritor, saida);
        }
    }

    /** Descomprime o que {@link #comprimir} produziu. */
    public static byte[] descomprimir(byte[] comprimido) throws IOException {
        try (var leitor = LeitorDeBits.de(comprimido)) {
            var quantidade = leitor.ler(32);

            if (quantidade == 0) {
                return new byte[0];
            }

            if (quantidade < 0) {
                throw new IOException("Cabeçalho inválido: tamanho negativo.");
            }

            var codigo = CodigoCanonico.lerTabela(leitor);
            var dados = new byte[quantidade];

            for (var i = 0; i < quantidade; i++) {
                dados[i] = (byte) codigo.ler(leitor);
            }

            return dados;
        }
    }

    /**
     * Quantos bytes a compressão ocuparia, sem de fato comprimir. É o que
     * permite escolher o método mais barato antes de gastar o trabalho.
     */
    public static long tamanhoEstimado(byte[] dados) {
        if (dados.length == 0) {
            return 4;
        }

        var frequencias = Frequencias.de(dados);
        var codigo = CodigoCanonico.de(ArvoreDeHuffman.comprimentos(frequencias));

        return 4 + CodigoCanonico.tamanhoDaTabela() + ((codigo.bitsPara(frequencias) + 7) / 8);
    }

    private static byte[] terminar(EscritorDeBits escritor, ByteArrayOutputStream saida) throws IOException {
        escritor.alinhar();
        return saida.toByteArray();
    }
}
