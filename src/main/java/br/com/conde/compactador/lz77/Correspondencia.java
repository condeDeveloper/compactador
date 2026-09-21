package br.com.conde.compactador.lz77;

/**
 * Um trecho que já apareceu antes: volte {@code distancia} bytes e copie
 * {@code comprimento} deles.
 *
 * <p>Comprimento zero significa que não houve repetição e o byte vai literal.
 */
public record Correspondencia(int distancia, int comprimento) {

    /** A ausência de repetição. */
    public static final Correspondencia NENHUMA = new Correspondencia(0, 0);

    public Correspondencia {
        if (distancia < 0 || comprimento < 0) {
            throw new IllegalArgumentException("Distância e comprimento não podem ser negativos.");
        }
    }

    /** Indica se vale a pena emitir uma referência em vez de um literal. */
    public boolean vale() {
        return comprimento >= Lz77.COMPRIMENTO_MINIMO;
    }

    @Override
    public String toString() {
        return vale() ? "<" + distancia + "," + comprimento + ">" : "-";
    }
}
