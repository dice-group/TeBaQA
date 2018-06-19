package de.uni.leipzig.tebaqa.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResultsetBindingTest {

    @Test
    public void testGetLongDateWithShortMonth() {
        ResultsetBinding rs = new ResultsetBinding();
        String actual = rs.getLongDate("1616-1-12");
        assertEquals("1616-01-12", actual);
    }

    @Test
    public void testGetLongDateWithShortDayAndMonth() {
        ResultsetBinding rs = new ResultsetBinding();
        String actual = rs.getLongDate("1616-1-1");
        assertEquals("1616-01-01", actual);
    }
}
