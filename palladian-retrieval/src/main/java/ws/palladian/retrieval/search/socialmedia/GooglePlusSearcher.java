package ws.palladian.retrieval.search.socialmedia;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for posts on Google+.
 * </p>
 * 
 * @see <a href="https://developers.google.com/+/api/latest/activities/search">API documentation for search</a>
 * @author Philipp Katz
 */
public final class GooglePlusSearcher extends AbstractSearcher<WebContent> {

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Google+";

    /** The pattern for parsing the returned dates. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SZ";

    /** The identifier for the {@link Configuration} key with the api key. */
    public static final String CONFIG_API_KEY = "api.googleplus.key";

    /** The API key for accessing Google+ API. */
    private final String apiKey;
    
    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new searcher for Google+.
     * </p>
     * 
     * @param apiKey The API key for accessing the service, not <code>null</code> or empty.
     */
    public GooglePlusSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Create a new searcher for Google+.
     * </p>
     * 
     * @param configuration The configuration instance providing an API key for accessing Google+ with the identifier
     *            {@value #CONFIG_API_KEY}, not <code>null</code>.
     */
    public GooglePlusSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> results = new ArrayList<>();

        String nextPageToken = null;

        out: for (;;) {
            String requestUrl = buildUrl(query, nextPageToken);
            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP error while accessing \"" + requestUrl + "\": "
                        + e.getMessage(), e);
            }

            String jsonString = httpResult.getStringContent();
            try {
                JsonObject jsonResult = new JsonObject(jsonString);
                JsonArray jsonItems = jsonResult.getJsonArray("items");
                nextPageToken = jsonResult.tryGetString("nextPageToken");
                if (nextPageToken == null) {
                    break;
                }
                for (int i = 0; i < jsonItems.size(); i++) {
                    JsonObject jsonItem = jsonItems.getJsonObject(i);
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();

                    builder.setUrl(jsonItem.tryGetString("url"));
                    builder.setTitle(jsonItem.tryGetString("title"));
                    builder.setSummary(jsonItem.tryGetString("content"));
                    builder.setPublished(getCreationDate(jsonItem.tryGetString("published")));
                    results.add(builder.create());
                    if (results.size() == resultCount) {
                        break out;
                    }
                }
            } catch (JsonException e) {
                throw new SearcherException("Error parsing the JSON response from \"" + requestUrl + "\": "
                        + jsonString, e);
            }
        }

        return results;
    }

    private String buildUrl(String query, String pageToken) {
        StringBuilder url = new StringBuilder();
        url.append("https://www.googleapis.com/plus/v1/activities");
        url.append("?query=").append(UrlHelper.encodeParameter(query));
        url.append("&key=").append(apiKey);
        if (pageToken != null) {
            url.append("&pageToken=").append(pageToken);
        }
        url.append("&maxResults=20"); // 20 is maximum
        return url.toString();
    }

    private Date getCreationDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        GooglePlusSearcher searcher = new GooglePlusSearcher("AIzaSyDPsLByNcOyrAFPlsldd8B2SoBHH3sywmo");
        List<WebContent> result = searcher.search("cat", 1000);
        CollectionHelper.print(result);
    }

}
