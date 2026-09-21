package br.com.conde.compactador;

/**
 * O resultado de uma compressão em números.
 *
 * @param metodo o método escolhido
 * @param original tamanho da entrada, em bytes
 * @param comprimido tamanho da saída, em bytes
 */
public record Estatisticas(Metodo metodo, int original, int comprimido) {

    /** Quanto sobrou, de 0 a 1. Abaixo de 1 houve ganho. */
    public double razao() {
        return original == 0 ? 1 : (double) comprimido / original;
    }

    /** Quanto foi economizado, em percentual. */
    public double economia() {
        return (1 - razao()) * 100;
    }

    /** Quantos bits cada byte da entrada custou no fim. */
    public double bitsPorByte() {
        return original == 0 ? 0 : comprimido * 8.0 / original;
    }

    /** Indica se a saída ficou menor que a entrada. */
    public boolean encolheu() {
        return comprimido < original;
    }

    @Override
    public String toString() {
        return String.format(
                java.util.Locale.ROOT,
                "%s: %d -> %d bytes (%.1f%% menor, %.2f bits/byte)",
                metodo, original, comprimido, economia(), bitsPorByte());
    }
}
