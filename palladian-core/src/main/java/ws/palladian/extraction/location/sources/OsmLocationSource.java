package ws.palladian.extraction.location.sources;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.extraction.location.*;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

import java.nio.charset.Charset;
import java.util.*;

/**
 * <p>
 * Location source from OpenStreetMap using the "Overpass API", which is run by third parties. See link below for
 * available servers.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://www.openstreetmap.org">OpenStreetMap</a>
 * @see <a href="http://wiki.openstreetmap.org/wiki/Overpass_API">Overpass API</a>
 * @see <a href="http://wiki.openstreetmap.org/wiki/Overpass_API/Language_Guide">Overpass API/Language Guide</a>
 * @see <a href="http://overpass-api.de">Overpass API</a>
 */
public class OsmLocationSource extends SingleQueryLocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OsmLocationSource.class);

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    private final String queryBaseUrl;

    private static final Map<String, LocationType> TYPE_MAPPING;

    static {
        // http://wiki.openstreetmap.org/wiki/Key:place
        TYPE_MAPPING = new HashMap<>();
        TYPE_MAPPING.put("city", LocationType.CITY);
        TYPE_MAPPING.put("town", LocationType.CITY);
        TYPE_MAPPING.put("village", LocationType.CITY);
        TYPE_MAPPING.put("hamlet", LocationType.CITY);
        TYPE_MAPPING.put("isolated_dwelling", LocationType.CITY);
        TYPE_MAPPING.put("farm", LocationType.POI);
        TYPE_MAPPING.put("suburb", LocationType.UNIT);
        TYPE_MAPPING.put("neighbourhood", LocationType.REGION);
        TYPE_MAPPING.put("continent", LocationType.CONTINENT);
        TYPE_MAPPING.put("country", LocationType.COUNTRY);
        TYPE_MAPPING.put("county", LocationType.UNIT);
        TYPE_MAPPING.put("island", LocationType.LANDMARK);
        TYPE_MAPPING.put("locality", LocationType.LANDMARK);
        TYPE_MAPPING.put("region", LocationType.UNIT);
        TYPE_MAPPING.put("state", LocationType.UNIT);
    }

    /** The default Overpass API endpoint to use. */
    public static final String DEFAULT_QUERY_BASE_URL = "http://overpass-api.de/api";

    /**
     * <p>
     * Create a new {@link OsmLocationSource} with the specified base URL.
     * </p>
     *
     * @param queryBaseUrl The base URL for queries, not <code>null</code> or empty.
     */
    public OsmLocationSource(String queryBaseUrl) {
        Validate.notEmpty(queryBaseUrl, "queryBaseUrl must not be empty");
        if (queryBaseUrl.endsWith("/")) { // trim trailing slash
            queryBaseUrl = queryBaseUrl.substring(0, queryBaseUrl.length() - 1);
        }
        this.queryBaseUrl = queryBaseUrl;
    }

    /**
     * <p>
     * Create a new {@link OsmLocationSource} accessing the service at {@value #DEFAULT_QUERY_BASE_URL}.
     * </p>
     */
    public OsmLocationSource() {
        this(DEFAULT_QUERY_BASE_URL);
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        String query = String.format("[out:json];(node[\"name\"~\"^%s$\",i][\"place\"];);out;", locationName);
        return performOverpassQuery(query);
    }

    private Collection<Location> performOverpassQuery(String query) {
        String queryUrl = String.format("%s/interpreter?data=%s", queryBaseUrl, UrlHelper.encodeParameter(query));
        try {
            LOGGER.debug("Requesting from {}", queryUrl);
            HttpResult result = retriever.httpGet(queryUrl);
            String content = new String(result.getContent(), Charset.forName("UTF-8"));
            JsonObject jsonObject = new JsonObject(content);
            JsonArray elementsJson = jsonObject.getJsonArray("elements");
            List<Location> locations = new ArrayList<>();
            for (int i = 0; i < elementsJson.size(); i++) {
                JsonObject jsonElement = elementsJson.getJsonObject(i);
                double lat = jsonElement.getDouble("lat");
                double lon = jsonElement.getDouble("lon");
                long id = jsonElement.getLong("id");
                // XXX stupid hack, we should change the type of IDs in Location to long
                int hack = Long.valueOf(id).hashCode();
                if (id != hack) {
                    LOGGER.warn("attn: ID {} was shortened to {}", id, hack);
                }
                JsonObject jsonTags = jsonElement.getJsonObject("tags");
                String name = jsonTags.getString("name");
                Set<AlternativeName> altNames = parseAlternativeNames(jsonTags);
                Long population = null;
                if (jsonTags.get("population") != null) {
                    population = jsonTags.getLong("population");
                }
                String placeType = jsonTags.getString("place");
                LocationType type = mapPlaceType(placeType);
                GeoCoordinate coordinate = GeoCoordinate.from(lat, lon);
                locations.add(new ImmutableLocation(hack, name, altNames, type, coordinate, population, null));
            }
            return locations;
        } catch (HttpException | JsonException e) {
            throw new IllegalStateException(e);
        }
    }

    private Set<AlternativeName> parseAlternativeNames(JsonObject jsonTags) throws JsonException {
        Set<AlternativeName> altNames = new HashSet<>();
        for (String key : jsonTags.keySet()) {
            if (key.startsWith("name:")) {
                String languageCode = key.substring(key.indexOf(':') + 1);
                Language language = Language.getByIso6391(languageCode);
                String name = jsonTags.getString(key);
                altNames.add(new AlternativeName(name, language));
            }
        }
        return altNames;
    }

    private static LocationType mapPlaceType(String placeType) {
        LocationType type = TYPE_MAPPING.get(placeType);
        return type != null ? type : LocationType.UNDETERMINED;
    }

    @Override
    public Location getLocation(int locationId) {
        String query = String.format("[out:json];(node(%s));out;", locationId);
        Collection<Location> result = performOverpassQuery(query);
        Iterator<Location> iterator = result.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static void main(String[] args) {
        LocationSource locationSource = new OsmLocationSource();
        Collection<Location> result = locationSource.getLocations("dresden", null);
        CollectionHelper.print(result);
        Location locationResult = locationSource.getLocation(240076989);
        System.out.println(locationResult);
        System.out.println(locationResult.getAlternativeNames());
    }

}
