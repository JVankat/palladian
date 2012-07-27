package ws.palladian.extraction.date.helper;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.ArchiveDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.KeywordDate;
import ws.palladian.helper.date.dates.StructureDate;
import ws.palladian.helper.date.dates.UrlDate;

public class DateArrayHelperTest {

    @Test
    public void testFilter() throws FileNotFoundException {
        List<ExtractedDate> dates = new ArrayList<ExtractedDate>();

        ExtractedDate now = new ExtractedDate();
        dates.add(now);
        dates.add(new ContentDate(now));
        dates.add(new UrlDate(now, "http://www.example.com"));
        dates.add(new ContentDate(now));
        dates.add(new ContentDate(now));
        dates.add(new ArchiveDate(now));
        dates.add(new StructureDate(now));

        assertEquals(7, dates.size());
        assertEquals(3, DateArrayHelper.filter(dates, ContentDate.class).size());
        assertEquals(1, DateArrayHelper.filter(dates, ArchiveDate.class).size());
        assertEquals(4, DateArrayHelper.filter(dates, KeywordDate.class).size());
        assertEquals(7, DateArrayHelper.filter(dates, ExtractedDate.class).size());
    }

    @Test
    public void testCluster() {
        List<ExtractedDate> dates = new ArrayList<ExtractedDate>();
        ExtractedDate date1 = DateParser.parseDate("2010-08-01", "YYYY-MM-DD");
        dates.add(date1);
        ExtractedDate date2 = DateParser.parseDate("2010-08-02", "YYYY-MM-DD");
        dates.add(date2);
        ExtractedDate date3 = DateParser.parseDate("2010-08-03", "YYYY-MM-DD");
        dates.add(date3);
        ExtractedDate date4 = DateParser.parseDate("2010-08-04", "YYYY-MM-DD");
        dates.add(date4);
        ExtractedDate date5 = DateParser.parseDate("2010-08-05", "YYYY-MM-DD");
        dates.add(date5);
        ExtractedDate date6 = DateParser.parseDate("2010-08-03", "YYYY-MM-DD");
        dates.add(date6);
        ExtractedDate date7 = DateParser.parseDate("2010-08-04", "YYYY-MM-DD");
        dates.add(date7);
        ExtractedDate date8 = DateParser.parseDate("2010-08-05", "YYYY-MM-DD");
        dates.add(date8);
        ExtractedDate date9 = DateParser.parseDate("2010-08-05", "YYYY-MM-DD");
        dates.add(date9);
        ExtractedDate date10 = DateParser.parseDate("2010-08-05", "YYYY-MM-DD");
        dates.add(date10);
        ExtractedDate date11 = DateParser.parseDate("2010-08-04", "YYYY-MM-DD");
        dates.add(date11);
        ExtractedDate date12 = DateParser.parseDate("2010-08-03", "YYYY-MM-DD");
        dates.add(date12);
        ExtractedDate date13 = DateParser.parseDate("2010-08-05", "YYYY-MM-DD");
        dates.add(date13);
        ExtractedDate date14 = DateParser.parseDate("2010-08-02", "YYYY-MM-DD");
        dates.add(date14);
        ExtractedDate date15 = DateParser.parseDate("2010-08-04", "YYYY-MM-DD");
        dates.add(date15);

        List<List<ExtractedDate>> clusters = DateArrayHelper.cluster(dates, DateExactness.DAY);

        assertEquals(5, clusters.size());

        // 1st cluster, contains 1 x "2010-08-01"
        assertEquals(1, clusters.get(0).size());
        assertEquals(date1, clusters.get(0).get(0));

        // 2nd cluster, contains 2 x "2010-08-02"
        assertEquals(2, clusters.get(1).size());
        assertEquals(date2, clusters.get(1).get(0));
        assertEquals(date14, clusters.get(1).get(1));

        // 3rd cluster, contains 3 x "2010-08-03"
        assertEquals(3, clusters.get(2).size());
        assertEquals(date3, clusters.get(2).get(0));
        assertEquals(date6, clusters.get(2).get(1));
        assertEquals(date12, clusters.get(2).get(2));

        // 4th cluster, contains 4 x "2010-08-04"
        assertEquals(4, clusters.get(3).size());
        assertEquals(date4, clusters.get(3).get(0));
        assertEquals(date7, clusters.get(3).get(1));
        assertEquals(date11, clusters.get(3).get(2));
        assertEquals(date15, clusters.get(3).get(3));

        // 5th cluster, contains 5 x "2010-08-05"
        assertEquals(5, clusters.get(4).size());
        assertEquals(date5, clusters.get(4).get(0));
        assertEquals(date8, clusters.get(4).get(1));
        assertEquals(date9, clusters.get(4).get(2));
        assertEquals(date10, clusters.get(4).get(3));
        assertEquals(date13, clusters.get(4).get(4));
    }

    @Test
    public void testCountDates() {
        ExtractedDate date1 = DateParser.parseDate("2010-08-01 12:00:00", RegExp.DATE_ISO8601_YMD_T);
        ExtractedDate date2 = DateParser.parseDate("2010-08-01 12:00:30", RegExp.DATE_ISO8601_YMD_T);
        ExtractedDate date3 = DateParser.parseDate("2010-08-01 12:30:30", RegExp.DATE_ISO8601_YMD_T);
        ExtractedDate date4 = DateParser.parseDate("2010-08-01 13:00:00", RegExp.DATE_ISO8601_YMD_T);
        ExtractedDate date5 = DateParser.parseDate("2010-08-02 12:00:00", RegExp.DATE_ISO8601_YMD_T);
        ExtractedDate date6 = DateParser.parseDate("2010-09-01 12:00:00", RegExp.DATE_ISO8601_YMD_T);
        ExtractedDate date7 = DateParser.parseDate("2011-09-01 12:00:00", RegExp.DATE_ISO8601_YMD_T);
        List<ExtractedDate> dateList = Arrays.asList(date1, date2, date3, date4, date5, date6, date7);

        assertEquals(5, DateArrayHelper.countDates(date1, dateList, DateExactness.YEAR));
        assertEquals(4, DateArrayHelper.countDates(date1, dateList, DateExactness.MONTH));
        assertEquals(3, DateArrayHelper.countDates(date1, dateList, DateExactness.DAY));
        assertEquals(2, DateArrayHelper.countDates(date1, dateList, DateExactness.HOUR));
        assertEquals(1, DateArrayHelper.countDates(date1, dateList, DateExactness.MINUTE));
        assertEquals(0, DateArrayHelper.countDates(date1, dateList, DateExactness.SECOND));
    }

}
