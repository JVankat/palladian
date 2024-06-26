package ws.palladian.extraction.location;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.helper.collection.CaseInsensitiveMap;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Location extractor based on <a href="http://www.opencalais.com/calaisAPI">OpenCalais</a> API.
 * </p>
 *
 * @author Philipp Katz
 */
public class OpenCalaisLocationExtractor extends LocationExtractor {

    // XXX plenty of copied code from OpenCalaisNer; inheritance structure does not allow common base though currently.

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCalaisLocationExtractor.class);

    /** The mapping between OpenCalais types and Palladian's {@link LocationType}. */
    private static final Map<String, LocationType> LOCATION_MAPPING;

    /** The maximum number of characters allowed to send per request (actually 100,000). */
    private final int MAXIMUM_TEXT_LENGTH = 90000;

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    static {
        Map<String, LocationType> temp = new HashMap<>();
        temp.put("continent", LocationType.CONTINENT);
        temp.put("city", LocationType.CITY);
        temp.put("country", LocationType.COUNTRY);
        temp.put("facility", LocationType.POI);
        temp.put("naturalfeature", LocationType.LANDMARK);
        temp.put("region", LocationType.REGION);
        temp.put("provinceorstate", LocationType.UNIT);
        LOCATION_MAPPING = CaseInsensitiveMap.from(temp);
    }

    private final String apiKey;

    public OpenCalaisLocationExtractor(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {

        List<LocationAnnotation> annotations = new ArrayList<>();

        List<String> textChunks = NerHelper.createSentenceChunks(inputText, MAXIMUM_TEXT_LENGTH);

        LOGGER.debug("sending " + textChunks.size() + " text chunks, total text length " + inputText.length());

        // since the offset is per chunk we need to add the offset for each new chunk to get the real position of the
        // entity in the original text
        int cumulatedOffset = 0;
        for (String textChunk : textChunks) {

            String response = null;

            try {

                HttpResult httpResult = getHttpResult(textChunk.toString());
                response = httpResult.getStringContent();

                JsonObject json = new JsonObject(response);

                for (String key : json.keySet()) {

                    JsonObject obj = json.getJsonObject(key);
                    if (obj.get("_typeGroup") != null && obj.getString("_typeGroup").equalsIgnoreCase("entities")) {

                        String entityName = obj.getString("name");
                        String entityTag = obj.getString("_type");

                        LocationType type = LOCATION_MAPPING.get(entityTag);
                        if (type == null) {
                            LOGGER.debug("Ignore type {}", entityTag);
                            continue;
                        }

                        int id = 0;
                        String name = entityName;
                        GeoCoordinate coordinate = null;
                        if (obj.get("resolutions") != null) {
                            JsonArray resolutions = obj.getJsonArray("resolutions");
                            if (resolutions.size() > 0) {
                                JsonObject firstResolution = resolutions.getJsonObject(0);
                                Double latitude = firstResolution.tryGetDouble("latitude");
                                Double longitude = firstResolution.tryGetDouble("longitude");
                                if (latitude != null && longitude != null) {
                                    coordinate = GeoCoordinate.from(latitude, longitude);
                                }
                                String idString = firstResolution.tryGetString("id");
                                id = idString != null ? idString.hashCode() : 0;
                                name = firstResolution.tryGetString("name");
                            }
                        }

                        if (obj.get("instances") != null) {
                            JsonArray instances = obj.getJsonArray("instances");

                            for (int i = 0; i < instances.size(); i++) {
                                JsonObject instance = instances.getJsonObject(i);

                                // take only instances that are as long as the entity name, this way we discard
                                // co-reference resolution instances
                                if (instance.getInt("length") == entityName.length()) {
                                    int offset = instance.getInt("offset");
                                    Annotation annotation = new ImmutableAnnotation(cumulatedOffset + offset, entityName, entityTag);
                                    Location location = new ImmutableLocation(id, name, type, coordinate, null);
                                    annotations.add(new LocationAnnotation(annotation, location));
                                }
                            }
                        }
                    }
                }

            } catch (HttpException e) {
                LOGGER.error("Error performing HTTP POST: {}", e.getMessage());
            } catch (JsonException e) {
                LOGGER.error("Could not parse the JSON response: {}, exception: {}", new Object[]{response, e.getMessage(), e});
            }

            cumulatedOffset += textChunk.length();
        }

        return annotations;
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.POST, "http://api.opencalais.com/tag/rs/enrich");
        requestBuilder.addHeader("x-calais-licenseID", apiKey);
        requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        requestBuilder.addHeader("Accept", "application/json");
        FormEncodedHttpEntity.Builder entityBuilder = new FormEncodedHttpEntity.Builder();
        entityBuilder.addData("content", inputText);
        entityBuilder.addData("paramsXML",
                "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:discardMetadata=\";\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\" c:externalID=\"calaisbridge\" c:submitter=\"calaisbridge\"></c:userDirectives><c:externalMetadata c:caller=\"GnosisFirefox\"/></c:params>");
        requestBuilder.setEntity(entityBuilder.create());
        return httpRetriever.execute(requestBuilder.create());
    }

    @Override
    public String getName() {
        return "OpenCalais NER (Locations)";
    }

    public static void main(String[] args) {
        OpenCalaisLocationExtractor extractor = new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5");
        String text = "Dresden (Saxony) and Berlin are cities in Germany which lies in Europe on planet Earth, the middle east is somewhere else";
        List<LocationAnnotation> detectedLocations = extractor.getAnnotations(text);
        CollectionHelper.print(detectedLocations);
    }

}
