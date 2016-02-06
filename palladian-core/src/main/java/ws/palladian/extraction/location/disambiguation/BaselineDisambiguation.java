package ws.palladian.extraction.location.disambiguation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.MultiMap;

/**
 * <p>
 * Baseline disambiguation using maximum population heuristic.
 * </p>
 * 
 * @author Philipp Katz
 */
public class BaselineDisambiguation implements LocationDisambiguation {

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {
        List<LocationAnnotation> result = new ArrayList<>();

        for (Annotation annotation : locations.keySet()) {
            Collection<Location> currentLocations = locations.get(annotation);
            Location selectedLocation = null;
            long maxPopulation = 0;
            for (Location location : currentLocations) {
                LocationType type = location.getType();
                if (type == LocationType.CONTINENT || type == LocationType.COUNTRY) {
                    selectedLocation = location;
                    break;
                } else if (location.getPopulation() != null && location.getPopulation() >= maxPopulation) {
                    selectedLocation = location;
                    maxPopulation = location.getPopulation();
                }
            }
            if (selectedLocation != null) {
                result.add(new LocationAnnotation(annotation, selectedLocation));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "BaselineDisambiguation";
    }

}
