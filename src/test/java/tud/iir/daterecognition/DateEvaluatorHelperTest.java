package tud.iir.daterecognition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.RegExp;

public class DateEvaluatorHelperTest {

    @Test
    public void testIsDateInRange() {

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        ExtractedDate date = new ExtractedDate("2010-01-01T12:30:30Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate(cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH))
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate("1990-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertFalse(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate("2090-11-12T00:00:00Z", RegExp.DATE_ISO8601_YMD_T[1]);
        assertFalse(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate("Nov 8, 2007", RegExp.DATE_USA_MMMM_D_Y[1]);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate("3.9.2010", RegExp.DATE_EU_D_MM_Y[1]);
        assertTrue(DateRaterHelper.isDateInRange(date));
        date = new ExtractedDate("2010-09", RegExp.DATE_ISO8601_YM[1]);
        assertTrue(DateRaterHelper.isDateInRange(date));

    }
}
