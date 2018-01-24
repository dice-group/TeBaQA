package de.uni.leipzig.tebaqa.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextUtilitiesTest {

    @Test
    public void testTrim() {
        String test = "    THIS string    contains    a    lot    of unnecessary spaces !   ";
        String actual = TextUtilities.trim(test);
        String expected = "THIS string contains a lot of unnecessary spaces !";
        assertEquals(expected, actual);
    }
}