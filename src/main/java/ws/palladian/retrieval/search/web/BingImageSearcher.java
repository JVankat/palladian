package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Bing Image search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingImageSearcher extends BaseBingSearcher<WebImageResult> {

    /**
     * @see BaseBingSearcher#BaseBingSearcher(String)
     */
    public BingImageSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher(PropertiesConfiguration)
     */
    public BingImageSearcher(PropertiesConfiguration configuration) {
        super(configuration);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher()
     */
    public BingImageSearcher() {
        super();
    }

    @Override
    public String getName() {
        return "Bing Images";
    }

    @Override
    protected WebImageResult parseResult(JSONObject currentResult) throws JSONException {
        String url = currentResult.getString("MediaUrl");
        int width = currentResult.getInt("Width");
        int height = currentResult.getInt("Height");
        String title = currentResult.getString("Title");
        WebImageResult webResult = new WebImageResult(url, title, width, height);
        return webResult;
    }

    @Override
    protected String getSourceType() {
        return "Image";
    }

    @Override
    protected int getDefaultFetchSize() {
        return 25;
    }

}
