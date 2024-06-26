package ws.palladian.extraction.location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Tagger;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * {@link Tagger} for extracting geographic coordinates from text. Supported are coordinates in DMS format (e.g.
 * '40°26′47″N'), in decimal format (e.g. '40.446195N'), and combinations thereof (e.g. '40° 26.7717'). The coordinates
 * are transformed to decimal format and can be obtained from {@link LocationAnnotation#getLocation()}.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://en.wikipedia.org/wiki/Geographic_coordinate_conversion">Geographic coordinate conversion</a>
 */
public final class CoordinateTagger implements Tagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateTagger.class);

    /** The name of the tag to be assigned. */
    public static final String TAG_NAME = "geoCoordinate";

    private static final String LEFT = "(?<!\\w)";
    private static final String RIGHT = "(?!\\w)";
    private static final String DEG = "([-+]?\\d{1,3}\\.\\d{1,10})([NSWE])?";
    private static final String SEP = "(?:,\\s?|\\s)";

    /** Only degrees, as real number. */
    private static final Pattern PATTERN_DEG = Pattern.compile(LEFT + "(" + DEG + ")" + SEP + "(" + DEG + ")" + RIGHT);

    /** DMS scheme, and/or combination with degrees. */
    private static final Pattern PATTERN_DMS = Pattern.compile(LEFT + "(" + GeoUtils.DMS + ")" + SEP + "(" + GeoUtils.DMS + ")" + RIGHT);

    /** The singleton instance of this class. */
    public static final CoordinateTagger INSTANCE = new CoordinateTagger();

    private CoordinateTagger() {
        // singleton
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<LocationAnnotation> annotations = new ArrayList<>();
        Matcher matcher = PATTERN_DEG.matcher(text);
        while (matcher.find()) {
            try {
                double lat = Double.parseDouble(matcher.group(2));
                double lng = Double.parseDouble(matcher.group(5));
                lat = "S".equals(matcher.group(3)) ? -lat : lat;
                lng = "W".equals(matcher.group(6)) ? -lng : lng;
                if (GeoUtils.isValidCoordinateRange(lat, lng)) {
                    annotations.add(createAnnotation(matcher.start(), matcher.group(), lat, lng));
                }
            } catch (NumberFormatException e) {
                LOGGER.debug("NumberFormatException while parsing " + matcher.group() + ": " + e.getMessage());
            }
        }

        matcher = PATTERN_DMS.matcher(text);
        while (matcher.find()) {
            try {
                double lat = GeoUtils.parseDms(matcher.group(1));
                double lng = GeoUtils.parseDms(matcher.group(6));
                if (GeoUtils.isValidCoordinateRange(lat, lng)) {
                    annotations.add(createAnnotation(matcher.start(), matcher.group(), lat, lng));
                }
            } catch (NumberFormatException e) {
                LOGGER.debug("NumberFormatException while parsing " + matcher.group() + ": " + e.getMessage());
            }
        }
        return annotations;
    }

    private static final LocationAnnotation createAnnotation(int start, String value, double latitude, double longitude) {
        GeoCoordinate coordinate = GeoCoordinate.from(latitude, longitude);
        Location location = new ImmutableLocation(0, value, LocationType.UNDETERMINED, coordinate, null);
        return new LocationAnnotation(start, value, location, 1.0);
    }

}
