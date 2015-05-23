package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.WebVideo;
import ws.palladian.retrieval.search.videos.VimeoSearcher;

public class VimeoSearcherTest {

    @Test
    public void testParseJson() throws Exception {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/vimeo.json"));
        List<WebVideo> videoResults = VimeoSearcher.parseVideoResult(new JsonObject(jsonString));
        assertEquals(50, videoResults.size());

        WebVideo result1 = videoResults.get(0);
        assertEquals("Matta - Release The Freq", result1.getTitle());
        // 2011-03-08 14:09:49
        assertEquals(1299611389000l, result1.getPublished().getTime());
        assertEquals("https://vimeo.com/20800127", result1.getUrl());
        assertEquals(246, (long)result1.getDuration());
        
        // long resultCount = VimeoSearcher.parseResultCount(jsonString);
        // assertEquals(94609, resultCount);
    }

}
