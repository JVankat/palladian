package ws.palladian.retrieval.search.images;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for images on <a href="http://www.panoramio.com">Panoramio</a>.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://www.panoramio.com/api/data/api.html">API documentation</a>
 */
public final class PanoramioSearcher extends AbstractMultifacetSearcher<WebImage> {

//    public class PanoramioImageResult extends BasicWebImage {
//
//        private final GeoCoordinate imageCoordinate;
//
//        public PanoramioImageResult(String pageUrl, String imageUrl, String title, int width, int height, Date date,
//                GeoCoordinate imageCoordinate) {
//            super(pageUrl, imageUrl, title, null, width, height, date);
//            this.imageCoordinate = imageCoordinate;
//        }
//
//        public GeoCoordinate getImageCoordinate() {
//            return imageCoordinate;
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder builder = new StringBuilder();
//            builder.append("PanoramioImageResult [imageCoordinate=");
//            builder.append(imageCoordinate);
//            builder.append(", toString()=");
//            builder.append(super.toString());
//            builder.append("]");
//            return builder.toString();
//        }
//
//    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PanoramioSearcher.class);

    private static final String REQUEST_URL = "http://www.panoramio.com/map/get_panoramas.php?set=%s&from=%s&to=%s&minx=%s&miny=%s&maxx=%s&maxy=%s&size=%s&mapfilter=%s";

    private static final String DATE_PARSE_FORMAT = "d MMM y";
    
    private static final String NAME = "Panoramio";

    private final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public SearchResults<WebImage> search(MultifacetQuery query) throws SearcherException {
        List<WebImage> result = CollectionHelper.newArrayList();

        Long totalResultCount = null;
        double[] boundingBox = GeoUtils.getBoundingBox(query.getCoordinate(), query.getRadius());
        
        for (int offset = 0; result.size() < query.getResultCount(); offset += 100) {
            String requestUrl = getRequestUrl(offset, boundingBox);
            LOGGER.debug("Requesting {}", requestUrl);

            String stringResult;
            try {
                HttpResult httpResult = httpRetriever.httpGet(requestUrl);
                stringResult = httpResult.getStringContent();
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while accessing '" + requestUrl + "': " + e.toString(), e);
            }

            try {
                JSONObject jsonResult = new JSONObject(stringResult);
                totalResultCount = jsonResult.getLong("count");
                JSONArray photosJson = jsonResult.getJSONArray("photos");
                if (photosJson.length() == 0) {
                    break;
                }
                for (int i = 0; i < photosJson.length(); i++) {
                    JSONObject photoJson = photosJson.getJSONObject(i);
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setTitle(photoJson.getString("photo_title"));
                    builder.setUrl(photoJson.getString("photo_url"));
                    builder.setImageUrl(photoJson.getString("photo_file_url"));
                    builder.setWidth(photoJson.getInt("width"));
                    builder.setHeight(photoJson.getInt("height"));
                    builder.setPublished(parseDate(photoJson.getString("upload_date")));
                    double lng = photoJson.getDouble("longitude");
                    double lat = photoJson.getDouble("latitude");
                    builder.setCoordinate(new ImmutableGeoCoordinate(lat, lng));
                    result.add(builder.create());
                    if (result.size() >= query.getResultCount()) {
                        break;
                    }
                }
            } catch (JSONException e) {
                throw new SearcherException("Error while parsing the JSON string '" + stringResult + "': "
                        + e.toString(), e);
            }
        }
        return new SearchResults<WebImage>(result, totalResultCount);
    }

    private static String getRequestUrl(int offset, double[] boundingBox) {
        double minX = boundingBox[1];
        double minY = boundingBox[0];
        double maxX = boundingBox[3];
        double maxY = boundingBox[2];
        return String.format(REQUEST_URL, "full", offset, offset + 100, minX, minY, maxX, maxY, "original", false);
    }

    private static Date parseDate(String string) {
        DateFormat format = new SimpleDateFormat(DATE_PARSE_FORMAT, Locale.ENGLISH);
        try {
            return format.parse(string);
        } catch (ParseException e) {
            LOGGER.warn("Error while parsing {}", string);
            return null;
        }
    }
    
    @Override
    public String getName() {
        return NAME;
    }

    public static void main(String[] args) throws SearcherException {
        // System.out.println(parseDate("22 January 2007"));
        PanoramioSearcher searcher = new PanoramioSearcher();
        MultifacetQuery query = new MultifacetQuery.Builder().setCoordinate(51.049259, 13.73836).setRadius(1)
                .setResultCount(100).create();
        SearchResults<WebImage> result = searcher.search(query);
        CollectionHelper.print(result);
    }

}
