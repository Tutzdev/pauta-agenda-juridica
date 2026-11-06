package dev.tutz.pauta.appointment;

/** O quanto um compromisso em aberto pede atenção hoje. */
public enum Alert {
    /** A data já passou e ninguém deu baixa: prazo vencido ou compromisso esquecido. */
    OVERDUE,
    /** Acontece ou vence hoje. */
    TODAY,
    /** Prazo que vence dentro da janela de alerta configurada. */
    UPCOMING,
    NONE
}
