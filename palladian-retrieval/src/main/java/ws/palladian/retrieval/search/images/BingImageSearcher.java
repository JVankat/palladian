package ws.palladian.retrieval.search.images;

import org.apache.commons.configuration.Configuration;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.BaseBingSearcher;

/**
 * <p>
 * Bing Image search.
 * </p>
 * 
 * @author Philipp Katz
 * @deprecated API is defunct, this is no longer working.
 */
@Deprecated
public final class BingImageSearcher extends BaseBingSearcher<WebImage> {

    /**
     * @see BaseBingSearcher#BaseBingSearcher(String)
     */
    public BingImageSearcher(String accountKey) {
        super(accountKey);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher(Configuration)
     */
    public BingImageSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    public String getName() {
        return "Bing Images";
    }

    @Override
    protected WebImage parseResult(JsonObject currentResult) throws JsonException {
        BasicWebImage.Builder builder = new BasicWebImage.Builder();
        builder.setUrl(currentResult.getString("SourceUrl"));
        builder.setImageUrl(currentResult.getString("MediaUrl"));
        builder.setWidth(currentResult.getInt("Width"));
        builder.setHeight(currentResult.getInt("Height"));
        builder.setTitle(currentResult.getString("Title"));
        return builder.create();
    }

    @Override
    protected String getSourceType() {
        return "Image";
    }

    @Override
    protected int getDefaultFetchSize() {
        return 50;
    }

}
