package ws.palladian.daterecognition;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.control.AllTests;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.dates.HeadDate;
import ws.palladian.daterecognition.technique.HTTPDateGetter;
import ws.palladian.daterecognition.technique.HeadDateGetter;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.web.DocumentRetriever;

public class DateGetterHelperTest {

    @Test
    public void testGetURLDate() throws Exception {
        if (AllTests.ALL_TESTS) {
            final String url1 = "http://www.example.com/2010-06-30/example.html";
            final String url2 = "http://www.zeit.de/sport/2010-06/example";
            final String url3 = "http://www.nytimes.com2010_06_30/business/economy/30leonhardt.html?hp";
            final String url4 = "http://www.example.com/2010/06/example.html";
            final String url5 = "http://www.example.com/2010_06_30/example.html";
            final String url6 = "http://www.example.com/2010_06/example.html";
            final String url7 = "http://www.example.com/2010.06.30/example.html";
            final String url8 = "http://www.example.com/2010.06/example.html";
            final String url9 = "http://www.example.com/text/2010.06.30.html";
            final String url10 = "http://www.example.com/text/2010/othertext/06_30/example.html";
            final String url11 = "http://www.example.com/text/2010/othertext/06/30/example.html";
            final String url12 = "http://www.example.com/text/2010/othertext/06/30example.html";
            final String url13 = "http://www.example.com/text/2010/other/text/06_30example.html";
            final String url14 = "http://www.example.com/text/othertext/20100630example.html";
            final String url15 = "http://www.guardian.co.uk/world/2002/sep/06/iraq.johnhooper";
            final String url16 = "http://www.gazettextra.com/news/2010/sep/23/abortion-issue-senate-races/";
            final String url17 = "http://www.tmcnet.com/news/2010/06/30/1517705.htm";

            // Cases with given day
            String time = "2010-06-30";
            URLDateGetter udg = new URLDateGetter();
            udg.setUrl(url1);
            assertEquals(url1, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url3);
            assertEquals(url3, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url5);
            assertEquals(url5, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url7);
            assertEquals(url7, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url9);
            assertEquals(url9, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url10);
            assertEquals(url10, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url11);
            assertEquals(url11, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url12);
            assertEquals(url12, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url13);
            assertEquals(url13, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url14);
            assertEquals(url14, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url15);
            assertEquals(url15, "2002-09-06", udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url16);
            assertEquals(url16, "2010-09-23", udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url17);
            assertEquals(url17, time, udg.getFirstDate().getNormalizedDateString());

            // Cases without given day, so day will be set to 1st
            time = "2010-06";
            udg.setUrl(url2);
            assertEquals(url2, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url4);
            assertEquals(url4, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url6);
            assertEquals(url6, time, udg.getFirstDate().getNormalizedDateString());
            udg.setUrl(url8);
            assertEquals(url8, time, udg.getFirstDate().getNormalizedDateString());
        }
    }

    @Test
    public void testGetDateFromString() throws Exception {

        ExtractedDate date;
        String text;

        // ISO8601_YMD_T
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("2010-07-02T19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02T21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 21:07:49+02:00", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02T16:37:49-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-12-31 22:37-02:30", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2011-01-01 01:07", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02T19", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), "2010-07-02 19:07", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49Z", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010-07-02 19:07:49.123", RegExp.DATE_ISO8601_YMD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/07/02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010.07.02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010.07.02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_07_02 19:07:49.123", RegExp.DATE_ISO8601_YMD_SEPARATOR_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_07_02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/07/02", RegExp.DATE_ISO8601_YMD_SEPARATOR);
        assertEquals(date.getDateString(), "2010-07-02", date.getNormalizedDateString());

        // ISO_YMD
        text = "2010-06-05";
        date = DateGetterHelper.getDateFromString(text, RegExp.DATE_ISO8601_YMD);
        assertEquals(text, date.getNormalizedDateString());

        // ISO_YM
        text = "2010-06";
        date = DateGetterHelper.getDateFromString(text, RegExp.DATE_ISO8601_YM);
        assertEquals(text, date.getNormalizedDateString());

        // ISO8601_YWD
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010-W29-5", RegExp.DATE_ISO8601_YWD);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YWD_T
        text = "2010-07-22 19:07:49";
        date = DateGetterHelper.getDateFromString("2010-W29-5T19:07:49.123", RegExp.DATE_ISO8601_YWD_T);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YW
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("2010-W29", RegExp.DATE_ISO8601_YW);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // ISO8601_YD
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010-203", RegExp.DATE_ISO8601_YD);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // URL_D
        text = "2010-06-30";
        date = DateGetterHelper.getDateFromString("2010.06.30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_06_30", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/06/30/", RegExp.DATE_URL_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/June/30/", RegExp.DATE_URL_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // URL
        text = "2010-06";
        date = DateGetterHelper.getDateFromString("2010.06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010_06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("2010/06", RegExp.DATE_URL);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // EU & USA
        text = "2010-07-25";
        date = DateGetterHelper.getDateFromString("25.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07/25/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("25. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-05";
        date = DateGetterHelper.getDateFromString("5.07.2010", RegExp.DATE_EU_D_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("7/5/2010", RegExp.DATE_USA_MM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("5. Juli 2010", RegExp.DATE_EU_D_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 5, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 5th, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("July 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Juli 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07.2010", RegExp.DATE_EU_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("June 2010", RegExp.DATE_EUSA_MMMM_Y);
        assertEquals(date.getDateString(), "2010-06", date.getNormalizedDateString());
        text = "0-07-25";
        date = DateGetterHelper.getDateFromString("25.07.", RegExp.DATE_EU_D_MM);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("25. Juli", RegExp.DATE_EU_D_MMMM);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07/25", RegExp.DATE_USA_MM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25th", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 25", RegExp.DATE_USA_MMMM_D);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("07/2010", RegExp.DATE_USA_MM_Y);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("09/8/10 04:56 PM", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(date.getDateString(), "2010-09-08 16:56", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("August 10, 2010", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals(date.getDateString(), "2010-08-10", date.getNormalizedDateString());

        // DATE_RFC & ANSI C
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("Tue Jul 2 19:07:49 2010", RegExp.DATE_ANSI_C);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tue Jul 2 15:37:49 2010 -03:30", RegExp.DATE_ANSI_C_TZ);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 19:07:49 GMT", RegExp.DATE_RFC_1123);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 19:07:49 GMT", RegExp.DATE_RFC_1036);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Wed, 08 Sep 2010 08:09:15 EST", RegExp.DATE_RFC_1123);
        assertEquals("2010-09-08 08:09:15", date.getNormalizedDateString());

        // ISO without separator
        text = "2010-07-25";
        date = DateGetterHelper.getDateFromString("20100725", RegExp.DATE_ISO8601_YMD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010W295", RegExp.DATE_ISO8601_YWD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07";
        date = DateGetterHelper.getDateFromString("2010W29", RegExp.DATE_ISO8601_YW_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());
        text = "2010-07-22";
        date = DateGetterHelper.getDateFromString("2010203", RegExp.DATE_ISO8601_YD_NO);
        assertEquals(date.getDateString(), text, date.getNormalizedDateString());

        // RFC + UTC
        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 20:07:49 +0100", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -0100", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tue, 02 Jul 2010 20:07:49 +01:00", RegExp.DATE_RFC_1123_UTC);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Tuesday, 02-Jul-10 18:07:49 -01:00", RegExp.DATE_RFC_1036_UTC);
        assertEquals(text, date.getNormalizedDateString());

        // EU & USA time

        text = "2010-07-02 19:07:49";
        date = DateGetterHelper.getDateFromString("02.07.2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02-07-2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02/07/2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02_07_2010 20:07:49 +0100", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("02. Juli 2010 20:07:49 +0100", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("07/02/2010 20:07:49 +0100", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("July 02nd, 2010 20:07:49 +0100", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals(text, date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("04.08.2006 / 14:52", RegExp.DATE_EU_D_MM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("08/04/2006 / 14:52", RegExp.DATE_USA_MM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("04 August 2006 / 14:52", RegExp.DATE_EU_D_MMMM_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("aug 4, 2006 / 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("aug 4, 2006 14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("aug 4, 2006  14:52", RegExp.DATE_USA_MMMM_D_Y_T);
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("Saturday, September 20. 2008", RegExp.DATE_USA_MMMM_D_Y);
        assertEquals("2008-09-20", date.getNormalizedDateString());
        date = DateGetterHelper.getDateFromString("11-12-2010 19:48:00", RegExp.DATE_USA_MM_D_Y_T_SEPARATOR);
        assertEquals("2010-11-12 19:48:00", date.getNormalizedDateString());

        // others
        text = "2010-07-02 19:07:49";
        assertEquals(text, DateGetterHelper.findDate("Tue, 02 Jul 2010 19:07:49 GMT").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("7/23/2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("23.7.2010 3:35:58 PM").getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("July 23rd, 2010 3:35:58 PM")
                .getNormalizedDateString());
        assertEquals("2010-07-23 15:35:58", DateGetterHelper.findDate("23. Juli 2010 3:35:58 PM")
                .getNormalizedDateString());
        assertEquals("2010-07-20 00:00:00", DateGetterHelper.findDate("Tue, 20 Jul 2010 00:00:00 Z")
                .getNormalizedDateString());
        assertEquals("2010-07-23 16:49:18", DateGetterHelper.findDate("Fri, 23 JUL 2010 16:49:18 AEST")
                .getNormalizedDateString());
        assertEquals("2010-07-24", DateGetterHelper.findDate("07/24/2010").getNormalizedDateString());
        assertEquals("2010-07-24", DateGetterHelper.findDate("Jul 24, 2010 EST").getNormalizedDateString());
        assertEquals("2010-07-25", DateGetterHelper.findDate("Sun, 25 Jul 2010").getNormalizedDateString());
        assertEquals("2010-03-07 22:53:50", DateGetterHelper.findDate("Sun 7 Mar 2010 10:53:50 PM GMT")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010, 8:20 PM GMT +07:00")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010 8:20 PM GMT +07:00")
                .getNormalizedDateString());
        assertEquals("2010-08-18 13:20", DateGetterHelper.findDate("Wednesday August 18, 2010, 8:20 PM +07:00")
                .getNormalizedDateString());
        assertEquals("2010-07-29", DateGetterHelper.findDate("29/07/2010").getNormalizedDateString());
        assertEquals("2010-09-07", DateGetterHelper.findDate("09/07/2010").getNormalizedDateString());
        assertEquals("2010-08-23", DateGetterHelper.findDate("Monday, August 23, 2010").getNormalizedDateString());
        assertEquals("2010-09-23", DateGetterHelper.findDate("Monday, Sep 23, 2010").getNormalizedDateString());

    }

    @Test
    public void testFindAllDates() {
        assertEquals("2010-01", (DateGetterHelper.findALLDates("Januar 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-02", (DateGetterHelper.findALLDates("Februar 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-03", (DateGetterHelper.findALLDates("März 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-04", (DateGetterHelper.findALLDates("April 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-05", (DateGetterHelper.findALLDates("Mai 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-06", (DateGetterHelper.findALLDates("Juni 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-07", (DateGetterHelper.findALLDates("Juli 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-08", (DateGetterHelper.findALLDates("August 2010")).get(0).getNormalizedDateString());
        assertEquals("MMMM YYYY", (DateGetterHelper.findALLDates("August 2010")).get(0).getFormat());
        assertEquals("2010-09", (DateGetterHelper.findALLDates("September 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-10", (DateGetterHelper.findALLDates("Oktober 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-11", (DateGetterHelper.findALLDates("November 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-12", (DateGetterHelper.findALLDates("Dezember 2010")).get(0).getNormalizedDateString());
        String date = (DateGetterHelper.findALLDates("SEPTEMBER 1, 2010")).get(0).getNormalizedDateString();
        assertEquals("2010-09-01", date);
        date = (DateGetterHelper.findALLDates(", 17/09/06 03:51:53")).get(0).getNormalizedDateString();
        assertEquals("2006-09-17 03:51:53", date);
        date = (DateGetterHelper.findALLDates("30.09.2010")).get(0).getNormalizedDateString();
        assertEquals("2010-09-30", date);
        date = (DateGetterHelper.findALLDates(", 08. Februar 2010, 17:15")).get(0).getNormalizedDateString();
        assertEquals("2010-02-08", date);
        date = (DateGetterHelper.findALLDates("Sept. 3, 2010")).get(0).getNormalizedDateString();
        assertEquals("2010-09-03", date);
        date = (DateGetterHelper.findALLDates("Last Modified: Wednesday, 11-Aug-2010 14:41:10 EDT")).get(0)
                .getNormalizedDateString();
        assertEquals("2010-08-11 14:41:10", date);
        date = (DateGetterHelper.findALLDates("JUNE 1, 2010")).get(0).getNormalizedDateString();
        assertEquals("2010-06-01", date);
        assertEquals("2010-01", (DateGetterHelper.findALLDates("jan. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-02", (DateGetterHelper.findALLDates("Feb. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-03", (DateGetterHelper.findALLDates("Mär. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-04", (DateGetterHelper.findALLDates("Apr. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-05", (DateGetterHelper.findALLDates("Mai. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-06", (DateGetterHelper.findALLDates("Jun. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-07", (DateGetterHelper.findALLDates("Jul. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-08", (DateGetterHelper.findALLDates("Aug. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-09", (DateGetterHelper.findALLDates("Sep. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-10", (DateGetterHelper.findALLDates("Okt. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-11", (DateGetterHelper.findALLDates("nov. 2010")).get(0).getNormalizedDateString());
        assertEquals("2010-12", (DateGetterHelper.findALLDates("Dez. 2010")).get(0).getNormalizedDateString());
        assertEquals("2007-12-06 17:37:45", (DateGetterHelper.findALLDates("2007-12-06T17:37:45Z")).get(0)
                .getNormalizedDateString());
        assertEquals("2008-12-06 17:37:45",
                (DateGetterHelper.findALLDates("2007-12-06T17:37:45Z 2008-12-06T17:37:45Z")).get(1)
                        .getNormalizedDateString());
        assertEquals("2008-09-20", (DateGetterHelper.findALLDates("Saturday, September 20, 2008")).get(0)
                .getNormalizedDateString());

    }

    @Test
    public void testFindDate() {
        String text = "2010-08-03";
        assertEquals(text, (DateGetterHelper.findDate("2010-08-03")).getNormalizedDateString());
        assertEquals("2002-08-06 03:08", (DateGetterHelper.findDate("2002-08-06T03:08BST")).getNormalizedDateString());
        assertEquals("2010-06", (DateGetterHelper.findDate("June 2010")).getNormalizedDateString());
        assertEquals("2010-08-31", (DateGetterHelper.findDate("Aug 31 2010")).getNormalizedDateString());
        assertEquals("2009-04-06 15:11",
                (DateGetterHelper.findDate("April  6, 2009  3:11 PM")).getNormalizedDateString());
        ExtractedDate date = DateGetterHelper.findDate("aug 4, 2006 / 14:52");
        assertEquals("2006-08-04 14:52", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2007-aug-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2007-aug.-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2007-August-12");
        assertEquals("2007-08-12", date.getNormalizedDateString());
        date = DateGetterHelper.findDate("2010/07/02");
        assertEquals("2010-07-02", date.getNormalizedDateString());

    }

    @Test
    public void testGetSeparator() {
        final String date1 = "2010-05-06";
        final String date2 = "2010_05_06";
        final String date3 = "2010.05.06";
        final String date4 = "2010/05/06";

        assertEquals("-", ExtractedDateHelper.getSeparator(date1));
        assertEquals("_", ExtractedDateHelper.getSeparator(date2));
        assertEquals("\\.", ExtractedDateHelper.getSeparator(date3));
        assertEquals("/", ExtractedDateHelper.getSeparator(date4));

    }

    @Test
    public void testGetDateparts() {
        final String[] referenz1 = { "2010", "06", "30" };
        final String[] referenz2 = { "93", "06", "14" };
        final String[] referenz3 = { "10", "06", "30" };

        final String[] date1 = { "2010", "06", "30" };
        final String[] date2 = { "30", "2010", "06" };
        final String[] date3 = { "06", "2010", "30" };
        final String[] date4 = { "06", "30", "2010" };
    }

    @Test
    public void testGetHTTPHeaderDate() {
        if (AllTests.ALL_TESTS) {
            System.out.println("testGetHTTPHeaderDate:");

            /*
             * ExtractedDate date = DateGetterHelper
             * .getHTTPHeaderDate("http://www.spreeblick.com/2010/07/08/william-shatner-hat-leonard-nimoys-fahrrad-geklaut/"
             * );
             */
            // String url = "http://www.zeit.de/politik/ausland/2010-09/russland-waldbraende-siedlungen";
            String url = "http://www.spreeblick.com/2010/07/08/william-shatner-hat-leonard-nimoys-fahrrad-geklaut/";

            HTTPDateGetter hdg = new HTTPDateGetter();
            hdg.setUrl(url);

            ArrayList<HTTPDate> dates = hdg.getDates();
            for (int i = 0; i < dates.size(); i++) {
                System.out.println(dates.get(i).getDateString());
            }
        }
    }

    @Test
    public void testGetStructureDate() {
        String url = DateGetterHelperTest.class.getResource("/webPages/webPageW3C.htm").getFile();
        String[] urlDates = { "2010-07-08T08:02:04-05:00", "2010-07-20T11:50:47-05:00", "2010-07-13T14:55:57-05:00",
                "2010-07-13T14:46:56-05:00", "2010-07-20", "2010-07-16", "2010-07-07" };
        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLStruct(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            Iterator<ExtractedDate> dateIterator = date.iterator();
            int index = 0;
            while (dateIterator.hasNext()) {
                final ExtractedDate extractedDate = dateIterator.next();

                assertEquals(urlDates[index], extractedDate.getDateString());
                index++;
            }
        }
    }

    @Test
    public void testGetStructureDate2() {

        if (AllTests.ALL_TESTS) {
            // String url = "http://www.aftonbladet.se/wendela/ledig/article3476060.ab";
            String url = "http://www.guardian.co.uk/world/2002/aug/06/iraq.johnhooper";

            if (!AllTests.ALL_TESTS) {
                ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
                DateGetter dateGetter = new DateGetter(url);
                dateGetter.setAllFalse();
                dateGetter.setTechHTMLStruct(true);
                ArrayList<ExtractedDate> dates = dateGetter.getDate();
                date.addAll(dates);
                DateArrayHelper.printDateArray(date);
            }
        }
    }

    @Test
    public void testGetContentDates() {
        if (AllTests.ALL_TESTS) {
            // final String url = "data/test/webPages/dateExtraction/kullin.htm";
            // String url =
            // "http://www.gatorsports.com/article/20100823/ARTICLES/100829802/1136?Title=Meyer-has-concerns-with-season-fast-approaching";
            // String url = "http://www.truthdig.com/arts_culture/item/20071108_mark_sarvas_on_the_hot_zone/";
            // String url =
            // "http://www.scifisquad.com/2010/05/21/fridays-sci-fi-tv-its-a-spy-game-on-stargate-universe?icid=sphere_wpcom_tagsidebar/";

            String url = "http://g4tv.com/games/pc/61502/star-wars-the-old-republic/index/";
            url = "data/evaluation/daterecognition/webpages/webpage_1292927985086.html";
            // String url =
            // "http://www.politicsdaily.com/2010/06/10/harry-reid-ads-tout-jobs-creation-spokesman-calls-sharron-angl/";
            if (AllTests.ALL_TESTS) {
                ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
                // date.addAll(DateGetterHelper
                // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
                // date.addAll(DateGetterHelper
                // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
                DateGetter dateGetter = new DateGetter(url);
                dateGetter.setAllFalse();
                dateGetter.setTechHTMLContent(true);
                ArrayList<ExtractedDate> dates = dateGetter.getDate();
                date.addAll(dates);
                DateArrayHelper.printDateArray(date);

            }
        }
    }

    @Ignore
    @Test
    public void testGetContentDates2() {
        final String url = DateGetterHelperTest.class.getResource("/webpages/dateExtraction/Bangkok.htm").getFile();

        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLContent(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);

        }
    }

    @Ignore
    @Test
    public void testGetDate() {
        String url = "src/test/resources/webPages/dateExtraction/alltop.htm";
        // url = "http://www.zeit.de/2010/36/Wirtschaft-Konjunktur-Deutschland";
        //url = "http://www.abanet.org/antitrust/committees/intell_property/standardsettingresources.html";
        if (AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllTrue();
            dateGetter.setTechReference(false);
            dateGetter.setTechArchive(false);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date, ExtractedDate.TECH_HTML_CONT);

        }
    }

    @Test
    public void testGetDate2() {
        final String url = "http://www.friendfeed.com/share?title=Google+displays+incorrect+dates+from+news+sites&link=http://www.kullin.net/2010/05/google-displays-incorrect-dates-from-news-sites/";

        if (AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLContent(true);
            dateGetter.setTechHTMLStruct(false);
            dateGetter.setTechReference(false);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);

        }
    }

    @Ignore
    @Test
    public void testGetReferenceDates() {
        String url = "http://www.spiegel.de/index.html";
        // String url = "data/test/webPages/dateExtraction/kullin.htm";

        if (AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllFalse();
            dateGetter.setTechReference(true);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            DateArrayHelper.printDateArray(date);

        }
    }

    @Test
    public void testGetHeadDates2() {
        if (AllTests.ALL_TESTS) {
            String url = "http://abclocal.go.com/wabc/story?section=news/local&id=6606410";

            DocumentRetriever c = new DocumentRetriever();
            c.setDocument(url);
            Document document = c.getDocument();

            HeadDateGetter hdg = new HeadDateGetter();
            hdg.setDocument(document);

            ArrayList<HeadDate> headDates = hdg.getDates();
            DateArrayHelper.printDateArray(headDates);
        }

    }

    @Test
    public void testGetHeadDates() {
        if (AllTests.ALL_TESTS) {
            String url = "src/test/resources/webpages/dateExtraction/zeit2.htm";
            ArrayList<HeadDate> compareDates = new ArrayList<HeadDate>();
            compareDates.add(new HeadDate("2010-09-03T09:43:13.211280+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
            compareDates.add(new HeadDate("2010-09-02T06:00:00+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
            compareDates.add(new HeadDate("2010-09-03T09:44:12.597203+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
            compareDates.add(new HeadDate("2010-09-03T09:41:54.059727+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
            compareDates.add(new HeadDate("2010-09-03T09:43:13.211280+00:00", RegExp.DATE_ISO8601_YMD_T[1]));
            compareDates.add(new HeadDate("2010-09-02T06:00:00+00:00", RegExp.DATE_ISO8601_YMD_T[1]));

            DocumentRetriever c = new DocumentRetriever();
            c.setDocument(url);
            Document document = c.getDocument();
            HeadDateGetter hdg = new HeadDateGetter();
            hdg.setDocument(document);
            ArrayList<HeadDate> headDates = hdg.getDates();
            assertEquals(6, headDates.size());
            for (int i = 0; i < headDates.size(); i++) {
                assertEquals(compareDates.get(i).getDateString(), headDates.get(i).getDateString());
            }
        }

    }

    @Test
    public void testArchiveDate() {
        if (AllTests.ALL_TESTS) {
            String url = "http://www.spiegel.de";
            DateGetter dg = new DateGetter(url);
            dg.setAllFalse();
            dg.setTechArchive(true);
            ArrayList<ExtractedDate> dates = dg.getDate();
            DateArrayHelper.printDateArray(dates);
        }
    }

    @Ignore
    @Test
    public void testFindRelativeDate() {
        String text = "5 days ago";
        ExtractedDate relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-11-26", relDate.getNormalizedDate(false));
        text = "114 days ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-08-09", relDate.getNormalizedDate(false));
        text = "4 month ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-08-03", relDate.getNormalizedDate(false));
        text = "12 month ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2009-12-06", relDate.getNormalizedDate(false));
        text = "1 year ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2009-12-01", relDate.getNormalizedDate(false));
        text = "11 years ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("1999-12-04", relDate.getNormalizedDate(false));
        text = "1 minute ago";
        relDate = DateGetterHelper.findRelativeDate(text);
        assertEquals("2010-12-01", relDate.getNormalizedDate(false));

    }
}
