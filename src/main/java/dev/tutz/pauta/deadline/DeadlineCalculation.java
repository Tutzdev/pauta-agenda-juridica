package dev.tutz.pauta.deadline;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado da contagem, com o que não contou para a advogada conferir.
 *
 * @param weekendDays sábados e domingos pulados
 * @param skipped     feriados e recesso pulados, agrupados em períodos (o recesso inteiro vira uma linha)
 */
public record DeadlineCalculation(LocalDate startDate, int businessDays, LocalDate dueDate, int weekendDays,
                                  List<SkippedPeriod> skipped) {

    public DeadlineCalculation {
        skipped = List.copyOf(skipped);
    }

    /** {@code from} e {@code to} iguais quando é um dia só. */
    public record SkippedPeriod(LocalDate from, LocalDate to, String reason) {

        SkippedPeriod extendTo(LocalDate date) {
            return new SkippedPeriod(from, date, reason);
        }
    }
}
