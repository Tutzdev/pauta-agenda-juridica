package dev.tutz.pauta.deadline;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import dev.tutz.pauta.shared.BusinessRuleException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deadlines")
public class DeadlineController {

    private static final int MAX_RANGE_DAYS = 92;

    private final DeadlineCalculator calculator;
    private final ForensicCalendar   calendar;

    public DeadlineController(DeadlineCalculator calculator, ForensicCalendar calendar) {
        this.calculator = calculator;
        this.calendar   = calendar;
    }

    @GetMapping("/calculate")
    public DeadlineCalculation calculate(@RequestParam LocalDate startDate, @RequestParam int businessDays) {
        return calculator.calculate(startDate, businessDays);
    }

    /** Feriados e recesso do período, para a agenda marcar os dias sem expediente. */
    @GetMapping("/closed-days")
    public List<ForensicCalendar.ClosedDay> closedDays(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BusinessRuleException("INVALID_RANGE",
                    "Consulte um período de no máximo " + MAX_RANGE_DAYS + " dias.");
        }
        return calendar.closedDays(from, to);
    }
}
