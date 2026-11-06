package dev.tutz.pauta.shared;

/** Normalização de texto livre vindo dos formulários. */
public final class Text {

    private Text() {
    }

    /** Remove espaços das pontas; texto vazio vira {@code null}, para não gravar "" no banco. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
