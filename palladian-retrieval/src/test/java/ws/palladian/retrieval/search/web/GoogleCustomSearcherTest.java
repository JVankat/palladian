package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.SearcherException;

public class GoogleCustomSearcherTest {

    @Test
    public void testParsing() throws JsonException, IOException {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/googleCustomSearchResponse.json"));
        List<WebContent> results = GoogleCustomSearcher.parse(jsonString);
        assertEquals(10, results.size());

        assertEquals("Palladian architecture - Wikipedia, the free encyclopedia", results.get(0).getTitle());
        assertEquals("http://en.wikipedia.org/wiki/Palladian_architecture", results.get(0).getUrl());
        assertEquals("Palladian architecture is a European style of architecture derived from the   designs of the Venetian architect Andrea Palladio (1508–1580). The term \"  Palladian\" ...", results.get(0).getSummary());

        long resultCount = GoogleCustomSearcher.parseResultCount(jsonString);
        assertEquals(147000, resultCount);
    }
    
    @Test
    public void testParsingErrorResponse() throws IOException {
        String jsonString = FileHelper.readFileToString(ResourceHelper
                .getResourceFile("/apiresponse/googleCustomSearchErrorResponse.json"));
        try {
            GoogleCustomSearcher.checkError(jsonString);
            fail();
        } catch (SearcherException e) {
            assertEquals("Error from Google Custom Search API: Invalid Value (400).", e.getMessage());
        }
    }

}
