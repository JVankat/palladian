package ws.palladian.retrieval.search.socialmedia;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for <a href="http://socialmention.com">Social Mention</a>. The API free access allows max. 100 queries/day.
 * 
 * @author pk
 * @see <a href="https://code.google.com/p/socialmention-api/wiki/APIDocumentation">API Documentation</a>
 */
public final class SocialMentionSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SocialMentionSearcher.class);

    /** The identifier for the config. key with the (optional) access key. */
    public static final String CONFIG_KEY = "api.socialmention.key";

    /** Name of the searcher. */
    private static final String NAME = "Social Mention";

    /** API key (optional). */
    private final String key;

    /**
     * <p>
     * Create a new SocialMentionSearcher.
     * 
     * @param key (optional) The API key to use, or <code>null</code>.
     */
    public SocialMentionSearcher(String key) {
        this.key = key;
    }

    /**
     * <p>
     * Create a new SocialMentionSearcher.
     * 
     * @param configuration The configuration, which can provide an (optional) key for accessing SocialMention via
     *            {@link #CONFIG_KEY}.
     */
    public SocialMentionSearcher(Configuration configuration) {
        Validate.notNull(configuration, "configuration must not be null");
        key = configuration.getString(CONFIG_KEY, null);
    }

    /**
     * <p>
     * Create a new SocialMentionSearcher with free API access (max. 100 queries/day).
     */
    public SocialMentionSearcher() {
        this.key = null;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        if (StringUtils.isBlank(query.getText())) {
            throw new SearcherException("The query must supply at least a text (MultifacetQuery#getText())");
        }
        StringBuilder queryUrl = new StringBuilder();
        queryUrl.append("http://socialmention.com/search");
        queryUrl.append("?q=").append(UrlHelper.encodeParameter(query.getText()));
        queryUrl.append("&f=json");
        queryUrl.append("&t[]=all");
        if (query.getLanguage() != null) {
            queryUrl.append("&lang=").append(query.getLanguage().getIso6391());
        }
        if (StringUtils.isNotBlank(key)) {
            queryUrl.append("&key=").append(key);
        }
        if (query.getStartDate() != null) {
            long fromTs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - query.getStartDate().getTime());
            queryUrl.append("&from_ts=").append(fromTs);
        }
        LOGGER.debug("GET {}", queryUrl);
        HttpResult httpResult;
        try {
            httpResult = HttpRetrieverFactory.getHttpRetriever().httpGet(queryUrl.toString());
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while getting \"" + queryUrl + "\"", e);
        }
        LOGGER.debug("JSON = {}", httpResult.getStringContent());
        try {
            JsonObject jsonResult = new JsonObject(httpResult.getStringContent());
            long count = jsonResult.getLong("count");
            List<WebContent> results = new ArrayList<>();
            JsonArray jsonItems = jsonResult.getJsonArray("items");
            for (int i = 0; i < Math.min(jsonItems.size(), query.getResultCount()); i++) {
                BasicWebContent.Builder builder = new BasicWebContent.Builder();
                JsonObject jsonItem = jsonItems.getJsonObject(i);
                builder.setTitle(jsonItem.getString("title"));
                builder.setSummary(jsonItem.getString("description"));
                builder.setUrl(jsonItem.getString("link"));
                builder.setPublished(new Date(jsonItem.getInt("timestamp") * 1000l));
                builder.setIdentifier(jsonItem.getString("id"));
                builder.setSource(NAME);
                results.add(builder.create());
            }
            return new SearchResults<WebContent>(results, count);
        } catch (JsonException e) {
            throw new SearcherException("Error while trying to parse JSON \"" + httpResult.getStringContent() + "\"", e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static void main(String[] args) throws SearcherException, JsonException {
        MultifacetQuery.Builder builder = new MultifacetQuery.Builder();
        builder.setText("tesla");
        builder.setLanguage(Language.ENGLISH);
        builder.setStartDate(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)));
        MultifacetQuery query = builder.create();
        SearchResults<WebContent> results = new SocialMentionSearcher().search(query);
        CollectionHelper.print(results);
    }

}
