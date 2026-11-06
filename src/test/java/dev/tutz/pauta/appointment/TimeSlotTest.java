package dev.tutz.pauta.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class TimeSlotTest {

    private final TimeSlot twoToThree = TimeSlot.of(LocalTime.of(14, 0), 60);

    @Test
    void overlappingSlotsConflict() {
        assertThat(twoToThree.overlaps(TimeSlot.of(LocalTime.of(14, 30), 30))).isTrue();
        assertThat(twoToThree.overlaps(TimeSlot.of(LocalTime.of(13, 0), 120))).isTrue();
    }

    @Test
    void backToBackSlotsDoNotConflict() {
        assertThat(twoToThree.overlaps(TimeSlot.of(LocalTime.of(15, 0), 60))).isFalse();
        assertThat(twoToThree.overlaps(TimeSlot.of(LocalTime.of(13, 0), 60))).isFalse();
    }

    @Test
    void aSlotThatWouldPassMidnightEndsAtTheEndOfTheDay() {
        TimeSlot late = TimeSlot.of(LocalTime.of(23, 0), 120);

        assertThat(late.end()).isEqualTo(LocalTime.of(23, 59));
        assertThat(late.overlaps(TimeSlot.of(LocalTime.of(23, 30), 15))).isTrue();
    }

    @Test
    void endIsStartPlusDuration() {
        assertThat(TimeSlot.of(LocalTime.of(9, 30), 90).end()).isEqualTo(LocalTime.of(11, 0));
    }
}
