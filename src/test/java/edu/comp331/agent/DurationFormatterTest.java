package edu.comp331.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class DurationFormatterTest {
    @Test void zero()               { assertEquals("0s", DurationFormatter.format(0)); }
    @Test void secondsOnly()        { assertEquals("45s", DurationFormatter.format(45)); }
    @Test void exactMinute()        { assertEquals("1m", DurationFormatter.format(60)); }
    @Test void minutesAndSeconds()  { assertEquals("1m 1s", DurationFormatter.format(61)); }
    @Test void exactHour()          { assertEquals("1h", DurationFormatter.format(3600)); }
    @Test void hoursAndSeconds()    { assertEquals("1h 1s", DurationFormatter.format(3601)); }
    @Test void allUnits()           { assertEquals("1h 2m 3s", DurationFormatter.format(3723)); }
    @Test void noDayRollover()      { assertEquals("27h 46m 40s", DurationFormatter.format(100000)); }
    @Test void longMaxValue()       { assertEquals("2562047788015215h 30m 7s", DurationFormatter.format(Long.MAX_VALUE)); }
    @Test void negativeRejected()   {
        assertThrows(IllegalArgumentException.class, () -> DurationFormatter.format(-1));
    }
}
