package dev.tutz.pauta.deadline;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deadlines")
public class DeadlineController {

    private final DeadlineCalculator calculator;

    public DeadlineController(DeadlineCalculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/calculate")
    public DeadlineCalculation calculate(@RequestParam LocalDate startDate, @RequestParam int businessDays) {
        return calculator.calculate(startDate, businessDays);
    }
}
