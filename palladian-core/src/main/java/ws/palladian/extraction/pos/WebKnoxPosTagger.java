package ws.palladian.extraction.pos;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * The WebKnoxPosTagger is equal to the PalladianPosTagger but is available through a REST API making the use of local
 * models unnecessary. See also here http://webknox.com/api#!/text/posTags_GET
 * </p>
 * 
 * @author David Urbansky
 */
public class WebKnoxPosTagger extends BasePosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WebKnoxPosTagger.class);
    
    /** The name of this POS tagger. */
    private static final String TAGGER_NAME = "Palladian POS Tagger";

    private final String appId;
    private final String apiKey;

    public WebKnoxPosTagger(String appId, String apiKey) {
        if (appId == null || appId.isEmpty()) {
            throw new IllegalArgumentException("The required App ID is missing.");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("The required API key is missing.");
        }
        this.appId = appId;
        this.apiKey = apiKey;
    }

    public WebKnoxPosTagger(Configuration configuration) {
        this(configuration.getString("api.webknox.appId"), configuration.getString("api.webknox.apiKey"));
    }

    public static void main(String[] args) {
        WebKnoxPosTagger palladianPosTagger = new WebKnoxPosTagger(ConfigHolder.getInstance().getConfig());
        System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

    @Override
    public void tag(List<Annotation> annotations) {
        StringBuilder text = new StringBuilder();
        for (Annotation annotation : annotations) {
            text.append(annotation.getValue()).append(" ");
        }
        
        DocumentRetriever retriever = new DocumentRetriever();
        String url = "http://webknox.com/api/text/posTags?text=";
        url += UrlHelper.urlEncode(text.toString().trim());
        url += "&appId=" + appId;
        url += "&apiKey=" + apiKey;
        JSONObject result = retriever.getJsonObject(url);

        String taggedText = "";
        try {
            taggedText = result.getString("taggedText");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

//        TagAnnotations tagAnnotations = new TagAnnotations();

        String[] words = taggedText.split("\\s");
        int i = 0;
        for (String word : words) {
            String[] parts = word.split("/");

            String tag = parts[1].toUpperCase();
            annotations.get(i).getFeatureVector().add(new NominalFeature(PROVIDED_FEATURE, tag));
            i++;
            
//            TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(parts[0]), tag,
//                    parts[0]);
//            tagAnnotations.add(tagAnnotation);
        }

        
    }

}
