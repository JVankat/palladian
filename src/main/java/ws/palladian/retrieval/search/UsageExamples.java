package ws.palladian.retrieval.search;

import java.util.List;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.web.BingSearcher;
import ws.palladian.retrieval.search.web.GoogleImageSearcher;
import ws.palladian.retrieval.search.web.WebImageResult;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;
import ws.palladian.retrieval.search.web.WebSearcherLanguage;

public class UsageExamples {

    public static void main(String[] args) throws SearcherException {

        // new searchers should be created by the SearcherFactory, which must be provided with a configuration
        Configuration config = ConfigHolder.getInstance().getConfig();

        // create a web searcher for the Bing search engine
        WebSearcher<WebResult> searcher = SearcherFactory.createSearcher(BingSearcher.class, config);
        // search for "Jim Carrey", 50 results, English language
        List<WebResult> webResults = searcher.search("Jim Carrey", 50, WebSearcherLanguage.ENGLISH);
        // print the results
        CollectionHelper.print(webResults);

        // create a web searcher to search for images on Google
        WebSearcher<WebImageResult> imageSearcher = SearcherFactory.createSearcher(GoogleImageSearcher.class, config);
        // search for ten images with "Jim Carrey"
        List<WebImageResult> imageResults = imageSearcher.search("Jim Carrey", 10);
        // print the results
        CollectionHelper.print(imageResults);

    }

}
