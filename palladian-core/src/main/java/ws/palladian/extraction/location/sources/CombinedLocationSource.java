package ws.palladian.extraction.location.sources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.*;

/**
 * <p>
 * {@link LocationSource} for combining multiple sources. Only retrieval by name and coordinate is allowed, because ID
 * retrieval makes no sense over multiple sources.
 * </p>
 *
 * @author Philipp Katz
 */
public final class CombinedLocationSource extends MultiQueryLocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CombinedLocationSource.class);

    public static enum QueryMode {
        /** Query all locations sources in the given order, in case of a match, remaining sources are not checked. */
        FIRST,
        /** Query all location sources and combine their results. */
        COMBINE
    }

    private final List<LocationSource> locationSources;
    private final QueryMode queryMode;
    private boolean showWarning = true;

    public CombinedLocationSource(QueryMode queryMode, Collection<LocationSource> locationSources) {
        this.queryMode = queryMode;
        this.locationSources = new ArrayList<LocationSource>(locationSources);
    }

    public CombinedLocationSource(QueryMode queryMode, LocationSource... locationSources) {
        this(queryMode, Arrays.asList(locationSources));
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        MultiMap<String, Location> result = DefaultMultiMap.createWithSet();
        for (LocationSource locationSource : locationSources) {
            MultiMap<String, Location> current = locationSource.getLocations(locationNames, languages);
            if (current.size() > 0) {
                result.addAll(current);
                if (queryMode == QueryMode.FIRST) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        throw new UnsupportedOperationException("Getting by IDs is not supported by " + getClass().getName());
    }

    @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        List<Location> result = new ArrayList<>();
        for (LocationSource locationSource : locationSources) {
            try {
                List<Location> locations = locationSource.getLocations(coordinate, distance);
                result.addAll(locations);
                if (locations.size() > 0 && queryMode == QueryMode.FIRST) {
                    break;
                }
            } catch (UnsupportedOperationException ignore) {
                if (showWarning) {
                    LOGGER.warn("LocationSource {} does not support reverse lookup.", locationSource);
                    showWarning = false;
                }
            }
        }
        Collections.sort(result, LocationExtractorUtils.distanceComparator(coordinate));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CombinedLocationSource [locationSources=");
        builder.append(locationSources);
        builder.append(", mode=");
        builder.append(queryMode);
        builder.append("]");
        return builder.toString();
    }

}
