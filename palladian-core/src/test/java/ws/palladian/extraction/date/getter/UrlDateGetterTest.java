package ws.palladian.extraction.date.getter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UrlDateGetterTest {

    @Test
    public void testGetUrlDate() {
        // Cases with given day
        String time = "2010-06-30";
        UrlDateGetter udg = new UrlDateGetter();
        String url = "http://www.example.com/2010-06-30/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.nytimes.com2010_06_30/business/economy/30leonhardt.html?hp";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/2010_06_30/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/2010.06.30/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/text/2010.06.30.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/text/2010/othertext/06_30/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/text/2010/othertext/06/30/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/text/2010/othertext/06/30example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/text/2010/other/text/06_30example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/text/othertext/20100630example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.guardian.co.uk/world/2002/sep/06/iraq.johnhooper";
        assertEquals("2002-09-06", udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.gazettextra.com/news/2010/sep/23/abortion-issue-senate-races/";
        assertEquals("2010-09-23", udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.tmcnet.com/news/2010/06/30/1517705.htm";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());

        // Cases without given day
        time = "2010-06";
        url = "http://www.zeit.de/sport/2010-06/example";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/2010/06/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/2010_06/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
        url = "http://www.example.com/2010.06/example.html";
        assertEquals(time, udg.getDates(url).get(0).getNormalizedDateString());
    }

}
