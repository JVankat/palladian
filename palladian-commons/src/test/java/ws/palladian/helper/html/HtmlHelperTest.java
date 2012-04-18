package ws.palladian.helper.html;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Test;

import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>Test cases for the {@link HtmlHelper} class.</p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class HtmlHelperTest {

    @Test
    public void testCountTags() {
        assertEquals(4, HtmlHelper.countTags("everybody is <b>here</b> to do some <p>work</p>"));
        assertEquals(4, HtmlHelper.countTags("<br />everybody is <b>here</b> to do some <p>work"));
        assertEquals(4, HtmlHelper.countTags("<br />everybody<br /> is <b>here</b> to do some <p>work", true));
        assertEquals(7, HtmlHelper.countTags("<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <p>work"));
        assertEquals(6, HtmlHelper.countTags(
                "<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <a>abc</a> <p>work", true));
    }

    @Test
    public void testCountTagLength() {
        assertEquals(0, HtmlHelper.countTagLength("iphone 4"));
        assertEquals(15, HtmlHelper.countTagLength("<phone>iphone 4</Phone>"));
        assertEquals(20, HtmlHelper.countTagLength("everybody is <b>here<br /></b> to do some <p>work</p>"));
    }

    @Test
    public void testStripTags() throws FileNotFoundException {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        assertEquals("this is relevant", HtmlHelper.stripHtmlTags(htmlContent, true, true, true, true).trim());

        String content = FileHelper.readFileToString(ResourceHelper.getResourceFile("removeHtmlTest.html"));
        String result = HtmlHelper.stripHtmlTags(content, true, true, true, false);
        
        assertEquals("65efc6cba6ae65e3e53e15c07e491fc4", DigestUtils.md5Hex(result));

//        String stripped = "Samsung S8500 Wave 3D view, 360&deg; spin GSMArena.com HomeNewsReviewsBlogRankingsCoverageSoftwareGlossaryFAQLinksContact us Advanced search Samsung S8500 Wave 3D view - 360&deg; spin Samsung S8500 Wave review: Hello, world!Samsung S8500 Wave preview: First lookMWC 2010: Samsung overviewSpecifications Read opinions Compare Pictures Related &nbsp;(new) Manual Check Price WElectronicsPlemixOmio (UK)Mobile City OnlineSelectGSM Popularity Daily interest 48% Total hits: 1266454 Voting results Design 9.1 Features 9.1 Performance 9.1 12345678910 12345678910 12345678910 Votes: 38011 &nbsp; Drag to rotate, double-click to spin 360&deg;. In order to see the 360&deg; rotation the Flash plugin is required. &nbsp; &nbsp; NokiaSamsungMotorolaSony EricssonLGAppleHTCi-mateO2EtenHPGarmin- AsusGigabyteAcerPalmBlackBerryMicrosoftVodafoneT-MobileSagemAlcatelPhilipsSharpToshibaBenQHuaweiPantechi-mobileZTEiNQMicromaxVertu more rumor mill Phone finder Home News Reviews Blog Forum Compare Links Glossary &nbsp;RSS feed &nbsp;Facebook Privacy policy Contact us &copy; 2000 - 2010 GSMArena.com team. Terms of use.";
//        result = result.replaceAll(System.getProperty("line.separator")," ");
        // System.out.println(DigestUtils.md5Hex(stripped));
//        Assert.assertEquals(DigestUtils.md5Hex(stripped), DigestUtils.md5Hex(result));
//        assertThat(result,is(stripped));
    }

//    @Test
//    public void testDocumentToReadableText() throws FileNotFoundException {
//        DocumentParser htmlParser = ParserFactory.createHtmlParser();
//        Document doc = htmlParser.parse(ResourceHelper.getResourceFile("/pageContentExtractor/test001.html"));
//        String result = HtmlHelper.documentToReadableText(doc);
//        Assert.assertEquals("489eb91cf94343d0b62e69c396bc6b6f", DigestUtils.md5Hex(result));
//    }

    @Test
    public void testReplaceHTMLSymbols() {
        String htmlText = "&nbsp; &Auml; &auml; &Ouml; &ouml; &Uuml; &uuml; &szlig; &lt; &gt; &amp; &quot;";
        String clearText = "  Ä ä Ö ö Ü ü ß < > & \"";
        assertEquals(clearText, StringHelper.replaceProtectedSpace(StringEscapeUtils.unescapeHtml(htmlText)));
        assertEquals(clearText, DateGetterHelper.replaceHtmlSymbols(htmlText));
    }

}
