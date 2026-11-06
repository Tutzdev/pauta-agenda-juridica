package dev.tutz.pauta.deadline;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.tutz.pauta.deadline.DeadlineCalculation.SkippedPeriod;
import dev.tutz.pauta.shared.BusinessRuleException;
import org.springframework.stereotype.Component;

/**
 * Conta prazos processuais em dias úteis (CPC, arts. 219 e 224): exclui o dia do começo,
 * inclui o do vencimento e só conta dias com expediente forense.
 */
@Component
public class DeadlineCalculator {

    static final int MAX_BUSINESS_DAYS = 365;

    private final ForensicCalendar calendar;

    public DeadlineCalculator(ForensicCalendar calendar) {
        this.calendar = calendar;
    }

    /**
     * @param startDate    data da intimação ou da publicação
     * @param businessDays tamanho do prazo em dias úteis
     */
    public DeadlineCalculation calculate(LocalDate startDate, int businessDays) {
        if (businessDays < 1 || businessDays > MAX_BUSINESS_DAYS) {
            throw new BusinessRuleException("INVALID_DEADLINE_LENGTH",
                    "O prazo precisa ter entre 1 e " + MAX_BUSINESS_DAYS + " dias úteis.");
        }

        List<SkippedPeriod> skipped = new ArrayList<>();
        boolean countedSinceLastSkip = true;
        int weekendDays = 0;
        int counted = 0;
        LocalDate day = startDate;

        while (counted < businessDays) {
            day = day.plusDays(1);
            Optional<String> closure = calendar.closureReason(day);
            if (closure.isEmpty()) {
                counted++;
                countedSinceLastSkip = true;
            } else if (ForensicCalendar.isWeekend(day)) {
                weekendDays++;
            } else {
                addSkipped(skipped, day, closure.get(), countedSinceLastSkip);
                countedSinceLastSkip = false;
            }
        }

        return new DeadlineCalculation(startDate, businessDays, day, weekendDays, skipped);
    }

    /** Dias seguidos com o mesmo motivo, separados só por fim de semana, viram um período. */
    private static void addSkipped(List<SkippedPeriod> skipped, LocalDate day, String reason,
            boolean countedSinceLastSkip) {

        int last = skipped.size() - 1;
        if (!countedSinceLastSkip && last >= 0 && skipped.get(last).reason().equals(reason)) {
            skipped.set(last, skipped.get(last).extendTo(day));
        } else {
            skipped.add(new SkippedPeriod(day, day, reason));
        }
    }
}
