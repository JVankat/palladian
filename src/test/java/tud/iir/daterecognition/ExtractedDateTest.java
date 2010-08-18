package tud.iir.daterecognition;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class ExtractedDateTest {

    private ExtractedDate date1;
    private ExtractedDate date2;
    private ExtractedDate date3;
    private ExtractedDate date4;
    private ExtractedDate date5;
    private ExtractedDate date6;
    private ExtractedDate date7;
    private ExtractedDate date8;
    private ExtractedDate date9;
    private ExtractedDate date10;
    private ExtractedDate date11;
    private ExtractedDate date12;
    private ExtractedDate date13;
    private ExtractedDate date14;
    private ExtractedDate date15;
    private ExtractedDate date16;
    private ExtractedDate date17;
    private ExtractedDate date18;

    @Before
    public void setUp() throws Exception {
        date1 = new ExtractedDate("2010-06-12", "yyyy-mm-dd");
        date2 = new ExtractedDate("10-06-07", "yyyy-mm-dd");
        date3 = new ExtractedDate("07.06.2010", "dd.mm.yyyy");
        date4 = new ExtractedDate("07.06.10", "dd.mm.yyyy");
        date5 = new ExtractedDate("06/07/2010", "mm/dd/yyyy");
        date6 = new ExtractedDate("06/07/10", "mm/dd/yyyy");
        date7 = new ExtractedDate("07. June 2010", "dd. mmmm yyyy");
        date8 = new ExtractedDate("June 07, 2010", "mmmm dd, yyyy");
        date9 = new ExtractedDate("07. June '10", "dd. mmmm yyyy");
        date10 = new ExtractedDate("2010_06_07", "yyyy_mm_dd");
        date11 = new ExtractedDate("2010.06.07", "yyyy_mm_dd");
        date12 = new ExtractedDate("2010/06/07", "yyyy_mm_dd");
        date13 = new ExtractedDate("june 10", "MMMM YYYY");
        date14 = new ExtractedDate("june 2010", "MMMM YYYY");
        date15 = new ExtractedDate("june '10", "MMMM YYYY");
        date16 = new ExtractedDate("mon, 07 jun 2010 07:06:05 GMT", "WD, DD MMM YYYY HH:MM:SS TZ");
        date17 = new ExtractedDate("Mondy, 07-jun-10 07:06:05 GMT", "WWD, DD-MMM-YY HH:MM:SS TZ");
        date18 = new ExtractedDate("mon jun 7 07:06:05 2010", "WD MMM DD_1 HH:MM:SS YYYY");
    }

    @Test
    public void testGetNormalizedDate() {
        assertEquals(date1.getDateString(), "2010-06-12", date1.getNormalizedDate());
        assertEquals(date2.getDateString(), "2010-06-07", date2.getNormalizedDate());
        assertEquals(date3.getDateString(), "2010-06-07", date3.getNormalizedDate());
        assertEquals(date4.getDateString(), "2010-06-07", date4.getNormalizedDate());
        assertEquals(date5.getDateString(), "2010-06-07", date5.getNormalizedDate());
        assertEquals(date6.getDateString(), "2010-06-07", date6.getNormalizedDate());
        assertEquals(date7.getDateString(), "2010-06-07", date7.getNormalizedDate());
        assertEquals(date8.getDateString(), "2010-06-07", date8.getNormalizedDate());
        assertEquals(date9.getDateString(), "2010-06-07", date9.getNormalizedDate());
        assertEquals(date10.getDateString(), "2010-06-07", date10.getNormalizedDate());
        assertEquals(date11.getDateString(), "2010-06-07", date11.getNormalizedDate());
        assertEquals(date12.getDateString(), "2010-06-07", date12.getNormalizedDate());
        assertEquals(date13.getDateString(), "2010-06", date13.getNormalizedDate());
        assertEquals(date14.getDateString(), "2010-06", date14.getNormalizedDate());
        assertEquals(date15.getDateString(), "2010-06", date15.getNormalizedDate());
        assertEquals(date16.getDateString(), "2010-06-07 07:06:05", date16.getNormalizedDate());
        assertEquals(date17.getDateString(), "2010-06-07 07:06:05", date17.getNormalizedDate());
        assertEquals(date18.getDateString(), "2010-06-07 07:06:05", date18.getNormalizedDate());
    }

    @Test
    public void testSetDateParts() {
        assertEquals(2010, date1.get(ExtractedDate.YEAR));
        assertEquals(6, date1.get(ExtractedDate.MONTH));
        assertEquals(12, date1.get(ExtractedDate.DAY));
        assertEquals(-1, date15.get(ExtractedDate.DAY));
        assertEquals(7, date16.get(ExtractedDate.HOUR));
        assertEquals(6, date16.get(ExtractedDate.MINUTE));
        assertEquals(5, date16.get(ExtractedDate.SECOND));
        assertEquals(7, date17.get(ExtractedDate.HOUR));
        assertEquals(6, date17.get(ExtractedDate.MINUTE));
        assertEquals(5, date17.get(ExtractedDate.SECOND));
        assertEquals(7, date18.get(ExtractedDate.HOUR));
        assertEquals(6, date18.get(ExtractedDate.MINUTE));
        assertEquals(5, date18.get(ExtractedDate.SECOND));

    }

}
