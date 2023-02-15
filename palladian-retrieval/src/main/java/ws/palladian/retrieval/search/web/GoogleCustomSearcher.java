package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
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
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Searcher for Google Custom Search. The free plan allows max. 100 queries/day. Although not obviously visible, the
 * search engine can be configured to search to <b>entire web</b>; see the links provided below for more information.
 * The searcher return max. 100 items per query.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="https://developers.google.com/custom-search/v1/overview">Overview Google Custom Search</a>
 * @see <a href="http://code.google.com/apis/console/?api=customsearch">Google API console</a>
 * @see <a href="http://www.google.com/cse">Google Custom Search settings</a>
 * @see <a href="http://support.google.com/customsearch/bin/answer.py?hl=en&answer=1210656">Search the entire web</a>
 */
public final class GoogleCustomSearcher extends AbstractMultifacetSearcher<WebContent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCustomSearcher.class);

    /** The name of this WebSearcher. */
    private static final String SEARCHER_NAME = "Google Custom Search";

    /** The identifier of the {@link Configuration} key for the api key. */
    public static final String CONFIG_API_KEY = "api.google.key";

    /** The identifier of the {@link Configuration} key for the search engine identifier. */
    public static final String CONFIG_SEARCH_ENGINE_IDENTIFIER = "api.google.customSearch.identifier";

    /** The API key for accessing the service. */
    private final String apiKey;

    /** The identifier of the Google Custom Search engine. */
    private final String searchEngineIdentifier;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Creates a new GoogleCustomSearcher.
     * </p>
     *
     * @param apiKey                 The API key for accessing Google Custom Search, not empty or <code>null</code>.
     * @param searchEngineIdentifier The identifier of the custom search, not empty or <code>null</code>.
     */
    public GoogleCustomSearcher(String apiKey, String searchEngineIdentifier) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        Validate.notEmpty(searchEngineIdentifier, "searchEngineIdentifier must not be empty");

        this.apiKey = apiKey;
        this.searchEngineIdentifier = searchEngineIdentifier;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Creates a new GoogleCustomSearcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key with the identifier {@value #CONFIG_API_KEY}
     *                      and a search engine identifier {@value #CONFIG_SEARCH_ENGINE_IDENTIFIER}.
     */
    public GoogleCustomSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY), configuration.getString(CONFIG_SEARCH_ENGINE_IDENTIFIER));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        List<WebContent> results = new ArrayList<>();
        Long resultCount = null;

        // Google Custom Search gives chunks of max. 10 items, and allows 10 chunks, i.e. max. 100 results.
        double numChunks = Math.min(10, Math.ceil((double) query.getResultCount() / 10));

        for (int start = 1; start <= numChunks; start++) {

            String searchUrl = createRequestUrl(query.getText(), start, Math.min(10, query.getResultCount() - results.size()), query.getLanguage());
            LOGGER.debug("Search with URL " + searchUrl);

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(searchUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP exception while accessing Google Custom Search with URL \"" + searchUrl + "\": " + e.getMessage(), e);
            }

            String jsonString = httpResult.getStringContent();
            checkError(jsonString);
            try {
                List<WebContent> current = parse(jsonString);
                if (current.isEmpty()) {
                    break;
                }
                results.addAll(current);
                if (resultCount == null) {
                    resultCount = parseResultCount(jsonString);
                }
            } catch (JsonException e) {
                throw new SearcherException("Error parsing the response from URL \"" + searchUrl + "\" (JSON was: \"" + jsonString + "\"): " + e.getMessage(), e);
            }
        }

        return new SearchResults<WebContent>(results, resultCount);
    }

    private String createRequestUrl(String query, int start, int num, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://www.googleapis.com/customsearch/v1");
        urlBuilder.append("?key=").append(apiKey);
        urlBuilder.append("&cx=").append(searchEngineIdentifier);
        urlBuilder.append("&q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&start=").append(start);
        urlBuilder.append("&num=").append(num);
        if (language != null) {
            urlBuilder.append("&lr=").append(getLanguageCode(language));
        }
        urlBuilder.append("&alt=json");
        return urlBuilder.toString();
    }

    private String getLanguageCode(Language language) {
        switch (language) {
            case ARABIC:
                return "lang_ar";
            case BULGARIAN:
                return "lang_bg";
            case CATALAN:
                return "lang_ca";
            case CZECH:
                return "lang_cs";
            case DANISH:
                return "lang_da";
            case GERMAN:
                return "lang_de";
            case GREEK:
                return "lang_el";
            case ENGLISH:
                return "lang_en";
            case SPANISH:
                return "lang_es";
            case ESTONIAN:
                return "lang_et";
            case FINNISH:
                return "lang_fi";
            case FRENCH:
                return "lang_fr";
            case CROATIAN:
                return "lang_hr";
            case HUNGARIAN:
                return "lang_hu";
            case INDONESIAN:
                return "lang_id";
            case ICELANDIC:
                return "lang_is";
            case ITALIAN:
                return "lang_it";
            case HEBREW:
                return "lang_iw";
            case JAPANESE:
                return "lang_ja";
            case KOREAN:
                return "lang_ko";
            case LITHUANIAN:
                return "lang_lt";
            case LATVIAN:
                return "lang_lv";
            case DUTCH:
                return "lang_nl";
            case NORWEGIAN:
                return "lang_no";
            case POLISH:
                return "lang_pl";
            case PORTUGUESE:
                return "lang_pt";
            case ROMANIAN:
                return "lang_ro";
            case RUSSIAN:
                return "lang_ru";
            case SLOVAK:
                return "lang_sk";
            case SLOVENE:
                return "lang_sl";
            case SERBIAN:
                return "lang_sr";
            case SWEDISH:
                return "lang_sv";
            case TURKISH:
                return "lang_tr";
            case CHINESE:
                return "lang_zh-CN";
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    static void checkError(String jsonString) throws SearcherException {
        if (StringUtils.isBlank(jsonString)) {
            throw new SearcherException("JSON response is empty.");
        }
        try {
            JsonObject jsonObject = new JsonObject(jsonString);
            JsonObject jsonError = jsonObject.getJsonObject("error");
            if (jsonError != null) {
                int errorCode = jsonError.getInt("code");
                String message = jsonError.getString("message");
                throw new SearcherException("Error from Google Custom Search API: " + message + " (" + errorCode + ").");
            }
        } catch (JsonException e) {
            throw new SearcherException("Could not parse JSON response ('" + jsonString + "').", e);
        }
    }

    /** default visibility for unit testing. */
    static List<WebContent> parse(String jsonString) throws JsonException {
        JsonObject jsonObject = new JsonObject(jsonString);
        JsonArray jsonItems = jsonObject.getJsonArray("items");
        if (jsonItems == null) {
            LOGGER.warn("JSON result did not contain an 'items' property. (JSON = '" + jsonString + "'.");
            return Collections.emptyList();
        }
        List<WebContent> result = new ArrayList<>();
        for (int i = 0; i < jsonItems.size(); i++) {
            JsonObject jsonItem = jsonItems.getJsonObject(i);
            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setTitle(jsonItem.getString("title"));
            builder.setUrl(jsonItem.getString("link"));
            builder.setSummary(jsonItem.getString("snippet"));
            builder.setSource(SEARCHER_NAME);
            result.add(builder.create());
        }
        return result;
    }

    /** default visibility for unit testing. */
    static long parseResultCount(String jsonString) throws JsonException {
        JsonObject jsonObject = new JsonObject(jsonString);
        return jsonObject.getJsonObject("searchInformation").getLong("totalResults");
    }

}
