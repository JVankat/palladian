package ws.palladian.retrieval.search.web;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * <p>
 * Google news search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleNewsSearcher extends BaseGoogleSearcher<WebResult> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/news";
    }

    @Override
    protected WebResult parseResult(JSONObject resultData) throws JSONException {
        String title = resultData.getString("titleNoFormatting");
        String content = resultData.getString("content");
        String url = resultData.getString("unescapedUrl");
        WebResult webResult = new WebResult(url, title, content);
        return webResult;
    }

    @Override
    public String getName() {
        return "Google News";
    }

}
