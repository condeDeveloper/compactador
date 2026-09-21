package br.com.conde.compactador;

/** Como os dados foram guardados. */
public enum Metodo {

    /** Sem compressão: os bytes vão como estão. */
    ARMAZENADO(0),

    /** Código de Huffman canônico. */
    HUFFMAN(1),

    /** Referência a trecho anterior. */
    LZ77(2);

    private final int codigo;

    Metodo(int codigo) {
        this.codigo = codigo;
    }

    /** O byte que identifica o método no cabeçalho. */
    public int codigo() {
        return codigo;
    }

    /** O método de um código do cabeçalho. */
    public static Metodo de(int codigo) {
        for (var metodo : values()) {
            if (metodo.codigo == codigo) {
                return metodo;
            }
        }

        throw new IllegalArgumentException("Método desconhecido: " + codigo);
    }
}
