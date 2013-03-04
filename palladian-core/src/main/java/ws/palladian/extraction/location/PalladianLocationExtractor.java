package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.InverseFilter;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

/**
 * <p>
 * Given a text, the LocationDetector finds mentioned locations and returns annotations.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLocationExtractor.class);

    // words that are unlikely to be a location
    private static final Set<String> skipWords;

    private final LocationSource locationSource;

    private final StopTokenRemover stopTokenRemover = new StopTokenRemover(Language.ENGLISH);

    public static final Map<String, Double> CASE_DICTIONARY;

    static {
        skipWords = new HashSet<String>();

        FileHelper.performActionOnEveryLine(
                PalladianLocationExtractor.class.getResourceAsStream("/locationsBlacklist.txt"), new LineAction() {
                    @Override
                    public void performAction(String line, int lineNumber) {
                        if (line.isEmpty() || line.startsWith("#")) {
                            return;
                        }
                        skipWords.add(line);
                    }
                });

        CASE_DICTIONARY = CollectionHelper.newHashMap();

        List<String> array;
        array = FileHelper.readFileToArray(PalladianLocationExtractor.class.getResourceAsStream("/caseDictionary.csv"));

        for (String string : array) {

            String[] parts = string.split("\t");
            String ratio = parts[3];
            if (ratio.equalsIgnoreCase("infinity")) {
                CASE_DICTIONARY.put(parts[0], 99999.0);
            } else {
                CASE_DICTIONARY.put(parts[0], Double.valueOf(ratio));
            }

        }

    }

    public PalladianLocationExtractor(LocationSource locationSource) {
        this.locationSource = locationSource;
    }

    @Override
    public Annotations getAnnotations(String text) {

        Annotations locationEntities = new Annotations();

        Annotations taggedEntities = StringTagger.getTaggedEntities(text);

        Set<Location> anchorLocations = CollectionHelper.newHashSet();

        // CollectionHelper.print(taggedEntities);

        filterPersonEntities(taggedEntities);
        filterNonEntities(taggedEntities, text);
        filterNonEntitiesWithCaseDictionary(taggedEntities, text);

        // CollectionHelper.print(taggedEntities);

        Set<Collection<Location>> ambiguousLocations = CollectionHelper.newHashSet();

        MultiMap<String, Location> locationMap = MultiMap.create();

        // try to find them in the database
        for (Annotation locationCandidate : taggedEntities) {

            String entityValue = locationCandidate.getEntity();

            entityValue = cleanName(entityValue);

            if (!StringHelper.isCompletelyUppercase(entityValue) && stopTokenRemover.isStopword(entityValue)) {
                continue;
            }

            if (skipWords.contains(entityValue)) {
                continue;
            }

            // search entities by name
            // List<Location> retrievedLocations = locationSource.retrieveLocations(entityValue);
            Collection<Location> retrievedLocations = locationSource.retrieveLocations(entityValue,
                    EnumSet.of(Language.ENGLISH));
            
            
            // FIXME make nicer
            // XXX check total length, and avoid checking long capitalized words which are no acronyms (AMERICA)
            if (StringHelper.isCompletelyUppercase(entityValue) || isAcronymSeparated(entityValue)) {

                LOGGER.debug("**** Acronym treatment : " + entityValue);

                Set<Location> temp = CollectionHelper.newHashSet();

                String temp1 = entityValue.replace(".", "");
                temp.addAll(locationSource.retrieveLocations(temp1, EnumSet.of(Language.ENGLISH)));
                String temp2 = makeAcronymSeparated(temp1);
                temp.addAll(locationSource.retrieveLocations(temp2, EnumSet.of(Language.ENGLISH)));

                retrievedLocations.clear();
                retrievedLocations.addAll(temp);
            }

            // if we retrieved locations with AND without coordinates, only keep those WITH coordinates
            Filter<Location> coordFilter = new Filter<Location>() {
                @Override
                public boolean accept(Location item) {
                    return item.getLatitude() != null && item.getLongitude() != null;
                }
            };
            HashSet<Location> temp = CollectionHelper.filter(retrievedLocations, coordFilter, new HashSet<Location>());
            if (temp.size() > 0) {
                retrievedLocations = temp;
            }

            // XXX experimental
            // greatly improves pr, but drops recall/f1
            //            CollectionHelper.filter(retrievedLocations, new Filter<Location>() {
            //                @Override
            //                public boolean accept(Location item) {
            //                    return item.getPopulation() > 0;
            //                }
            //            });

            if (retrievedLocations.isEmpty()) {
                continue;
            }
            for (Location location : retrievedLocations) {
                if (EnumSet.of(LocationType.CONTINENT, LocationType.COUNTRY).contains(location.getType())) {
                    anchorLocations.add(location);
                }
                // XXX experimental : add places with high population count to
                // anchor locations. we should determine how to set a good threshold here.
                // improves recall/f1, slightly drops precision
                if (location.getPopulation() > 500000) {
                    LOGGER.debug("High prob location " + location);
                    anchorLocations.add(location);
                }

            }

            boolean ambiguous = checkAmbiguity(retrievedLocations);
            if (ambiguous) {
                ambiguousLocations.add(retrievedLocations);
                LOGGER.debug("- " + entityValue + " is ambiguous!");
            } else {
                LOGGER.debug("+ " + entityValue + " is not amiguous: " + retrievedLocations);
            }

            if (!locationMap.containsKey(entityValue)) {
                locationMap.addAll(entityValue, retrievedLocations);
            }

            Location location = selectLocation(retrievedLocations);


            // CategoryEntries categoryEntries = new CategoryEntries();
            // categoryEntries.add(new CategoryEntry(location.getType().toString(), 1));
            // locationCandidate.setTags(categoryEntries);

            // locationEntities.add(locationCandidate);
            LocationAnnotation locationAnnotation = new LocationAnnotation(locationCandidate, location);
            locationEntities.add(locationAnnotation);

            if (!ambiguous && entityValue.split("\\s").length >= 3) {
                LOGGER.debug("Adding {} to anchor locations, because of long name", location.getPrimaryName());
                anchorLocations.add(location);
            }
        }


        // cluster(anchorLocations, locationMap);
        disambiguate(new HashSet<Location>(anchorLocations), locationMap);

        Set<Location> consolidatedLocations = CollectionHelper.newHashSet();
        consolidatedLocations.addAll(anchorLocations);
        for (List<Location> temp : locationMap.values()) {
            consolidatedLocations.addAll(temp);
        }

        Map<String, Location> finalResultsForCheck = CollectionHelper.newHashMap();

        Iterator<Annotation> iterator = locationEntities.iterator();
        while (iterator.hasNext()) {
            Annotation annotation = iterator.next();
            String entityValue = annotation.getEntity();

            entityValue = cleanName(entityValue);

            if (!locationMap.containsKey(entityValue)) {
                iterator.remove();
                continue;
            }
            if (locationMap.get(entityValue).size() == 0) {
                iterator.remove();
                continue;
            }
            if (locationMap.get(entityValue).size() > 1) {
                LOGGER.debug("Ambiguity for {}", entityValue);
            }
            Location loc = selectLocation(locationMap.get(entityValue));

            // XXX exp.
            //            if (loc.getPopulation() == 0 && loc.getType() != LocationType.CONTINENT
            //                    && loc.getType() != LocationType.COUNTRY) {
            //                boolean proximityCheck = checkProximity(loc, consolidatedLocations, anchorLocations);
            //                // System.err.println("unsure about " + loc + " : " + proximityCheck);
            //                if (!proximityCheck) {
            //                    LOGGER.info("Removing small location {} after proximity check", entityValue);
            //                    iterator.remove();
            //                    continue;
            //                }
            //            }

            CategoryEntries ces = new CategoryEntries();
            ces.add(new CategoryEntry(loc.getType().toString(), 1.));
            annotation.setTags(ces);

            finalResultsForCheck.put(annotation.getEntity(), loc);
        }

        Map<String, Location> clearMap = checkFinalResults(finalResultsForCheck, anchorLocations);
        iterator = locationEntities.iterator();
        while (iterator.hasNext()) {
            Annotation current = iterator.next();
            if (clearMap.containsKey(current.getEntity())) {
                LOGGER.debug("- remove - " + current);
                iterator.remove();
            }
        }

        // last step, recognize streets. For also extracting ZIP codes, this needs to be better integrated into above's
        // workflow. We should use the CITY annotations, to search for neighboring ZIP codes.
        List<Annotation> annotatedStreets = AddressTagger.tag(text);
        locationEntities.addAll(annotatedStreets);

        return locationEntities;
    }

    private Map<String, Location> checkFinalResults(Map<String, Location> finalResultsForCheck,
            Set<Location> anchorLocations) {
        List<Entry<String, Location>> locationList = new ArrayList<Entry<String, Location>>(
                finalResultsForCheck.entrySet());
        Map<String, Location> toClear = CollectionHelper.newHashMap();
        for (int i = 0; i < locationList.size(); i++) {
            Location l1 = locationList.get(i).getValue();
            if (l1.getType() == LocationType.CONTINENT || l1.getType() == LocationType.COUNTRY
                    || l1.getType() == LocationType.REGION) {
                continue;
            }
            if (anchorLocations.contains(l1)) {
                continue; // always accepted.
            }
            double smallestDistance = Double.MAX_VALUE;
            Location smallestLoc = null;
            for (int j = 0; j < locationList.size(); j++) {
                Location l2 = locationList.get(j).getValue();
                if (l1.equals(l2)) {
                    continue;
                }
                double distance = getDistance(l1, l2);
                if (smallestDistance > distance) {
                    smallestDistance = distance;
                    smallestLoc = l2;
                }
            }
            if (l1.getPopulation() == null || l1.getPopulation() < 5000) {
                LOGGER.debug(l1.getPrimaryName() + " : " + smallestDistance + " --- " + smallestLoc);
                if (smallestDistance > 250) {
                    toClear.put(locationList.get(i).getKey(), l1);
                }
            }
        }
        return toClear;
    }

    public String cleanName(String entityValue) {
        entityValue = entityValue.replace("®", "");
        entityValue = entityValue.replace("™", "");
        entityValue = entityValue.replace("\\s+", " ");
        entityValue = entityValue.trim();
        return entityValue;
    }

    public static boolean isAcronymSeparated(String string) {
        return string.matches("([A-Z]\\.)+");
    }

    private static String makeAcronymSeparated(String entityValue) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < entityValue.length(); i++) {
            result.append(entityValue.charAt(i));
            result.append('.');
        }
        return result.toString();
    }

    private boolean checkProximity(Location loc, Set<Location> anchorLocations2) {
        double closesProximity = Double.MAX_VALUE;
        Location closestLoc = null;
//        if (sureAnchors.contains(loc)) {
//            return true; // trivial.
//        }
        for (Location location : anchorLocations2) {
//            if (location.getPopulation() == 0) {
//                continue; // ignore them of course.
//            }
            double distance = getDistance(loc, location);
            if (distance < closesProximity) {
                closesProximity = distance;
                closestLoc = location;
            }
            // if (distance < 100) {
            //            if (distance < 500) {
            //                System.out.println("match for " + loc + " and " + location);
            //                return true;
            //            }
        }
        if (closestLoc != null) {
            LOGGER.debug("Closest prox. for " + loc.getPrimaryName() + " : " + closesProximity + "("
                    + closestLoc.getPrimaryName() + ")");
        }
        // return closesProximity < 500;
        return closesProximity < 50;
        //        return false;
    }

    //    private Collection<Location> getByType(Collection<Location> locations, final LocationType type) {
    //        return CollectionHelper.filter(locations, new Filter<Location>() {
    //            @Override
    //            public boolean accept(Location item) {
    //                return item.getType() == type;
    //            }
    //        }, new HashSet<Location>());
    //    }

//    private void cluster(Set<Location> anchorLocations, MultiMap<String, Location> ambiguousLocations) {
//
//        Set<Location> toAdd = CollectionHelper.newHashSet();
//        for (Location location : anchorLocations) {
//            List<Location> hierarchy = locationSource.getHierarchy(location.getId());
//            for (Location currentLocation : hierarchy) {
//                if (currentLocation.getPrimaryName().equalsIgnoreCase("earth")) {
//                    continue;
//                }
//                toAdd.add(currentLocation);
//            }
//        }
//        anchorLocations.addAll(toAdd);
//
//        // if we have countries as anchors, we remove the continents, to be more precise.
//        LocationTypeFilter countryFilter = new LocationTypeFilter(LocationType.COUNTRY);
//        if (CollectionHelper.filter(anchorLocations, countryFilter, new HashSet<Location>()).size() > 0) {
//            CollectionHelper.filter(anchorLocations, countryFilter);
//        }
//
//        // go through each group
//        for (String locationName : ambiguousLocations.keySet()) {
//            List<Location> locationsToCheck = ambiguousLocations.get(locationName);
//
//
//            for (Location location : anchorLocations) {
//
//                double closest = Double.MAX_VALUE;
//                Location closestLocation = null;
//
//                for (Location location2 : locationsToCheck) {
//                    double distance = getDistance(location, location2);
//                    if (distance < closest) {
//                        closest = distance;
//                        closestLocation = location2;
//                    }
//                    // System.out.println("Distance between " + location + " and " + location2 + " : " + distance);
//                }
//
//                if (closestLocation != null) {
//                    System.out.println("Distance between " + location + " and " + closestLocation + " : " + closest);
//                }
//            }
//        }
//
//    }

    private void disambiguate(Set<Location> anchorLocations, MultiMap<String, Location> ambiguousLocations) {

        Set<Location> toAdd = CollectionHelper.newHashSet();
        for (Location location : anchorLocations) {
            List<Location> hierarchy = locationSource.getHierarchy(location.getId());
            for (Location currentLocation : hierarchy) {
                if (currentLocation.getPrimaryName().equalsIgnoreCase("earth")) {
                    continue;
                }
                toAdd.add(currentLocation);
            }
        }
        anchorLocations.addAll(toAdd);

        Set<Location> fineAnchors = new HashSet<Location>(toAdd);

        // if we have countries as anchors, we remove the continents, to be more precise.
        LocationTypeFilter countryFilter = new LocationTypeFilter(LocationType.COUNTRY);
        if (CollectionHelper.filter(anchorLocations, countryFilter, new HashSet<Location>()).size() > 0) {
            CollectionHelper.filter(anchorLocations, countryFilter);
        }
        // if (getByType(anchorLocations, LocationType.COUNTRY).size() > 0) {
        // CollectionHelper.filter(anchorLocations, new LocationTypeFilter(LocationType.COUNTRY));
        //            CollectionHelper.filter(anchorLocations, new Filter<Location>() {
        //                @Override
        //                public boolean accept(Location item) {
        //                    return item.getType() == LocationType.COUNTRY;
        //                }
        //            });
        // }

        if (anchorLocations.size() == 0) {
            LOGGER.debug("No anchor locations");
            return;
        }

        fineAnchors.removeAll(anchorLocations);
        CollectionHelper.filter(fineAnchors, InverseFilter.create(new LocationTypeFilter(LocationType.COUNTRY)));
        CollectionHelper.filter(fineAnchors, InverseFilter.create(new LocationTypeFilter(LocationType.CONTINENT)));

        LOGGER.debug("Anchor locations: {}", anchorLocations);
        // CollectionHelper.print(anchorLocations);

        LOGGER.debug("Fine anchors: {}", fineAnchors);
        // CollectionHelper.print(fineAnchors);

        // Set<Location> positive = CollectionHelper.newHashSet();
        // Set<Location> negative = CollectionHelper.newHashSet();

        // go through each group
        for (String locationName : ambiguousLocations.keySet()) {

            LOGGER.debug(locationName);

            List<Location> list = ambiguousLocations.get(locationName);
            Set<Location> temp = CollectionHelper.newHashSet();

            // check each location in group
            Iterator<Location> it = list.iterator();
            while (it.hasNext()) {

                Location location = it.next();

                boolean anchored = false;
                List<Location> hierarchy = locationSource.getHierarchy(location.getId());

                // XXX experimental code; also keep locations without hierarchy
                if (hierarchy.isEmpty()) {
                    anchored = true;
                    // anchored = checkProximity(location, originalAnchors);
                    // anchored = checkProximity(location, fineAnchors);
                }
                // //

                for (Location anchorLocation : anchorLocations) {
                    if (hierarchy.contains(anchorLocation)) {
                        anchored = true;
                    }
                }

                // trivial case
                if (anchorLocations.contains(location)) {
                    anchored = true;
                }

                if (location.getType() == LocationType.CONTINENT) {
                    anchored = true;
                }

                LOGGER.debug(anchored + " -> " + location);

                if (!anchored) {
                    it.remove();
                }

//                if (anchored) {
//                    positive.add(location);
//                } else {
//                    temp.add(location);
//                }

            }

            // did we remove all? give a second chance below
//            if (list.isEmpty()) {
//                negative.addAll(temp);
//            }

            LOGGER.debug("-----------");
        }

        // go again through the negative locations and check,if we get them by proximity
//        for (Location negativeLocation : negative) {
//            for (Location positiveLocation : positive) {
//                double distance = getDistance(negativeLocation, positiveLocation);
//                if (distance < 100) {
//                    System.err.println("*** Re-add negative location because of distance " + distance + " : "
//                            + negativeLocation);
//                }
//            }
//        }

    }

    private void filterNonEntitiesWithCaseDictionary(Annotations taggedEntities, String text) {
        Iterator<Annotation> iterator = taggedEntities.iterator();
        while (iterator.hasNext()) {
            Annotation current = iterator.next();
            String value = current.getEntity();

            Double ratio = CASE_DICTIONARY.get(value.toLowerCase());
            if (ratio != null && ratio > 1.0) {
                iterator.remove();
                LOGGER.debug("remove " + value + " because of lc/uc ratio of " + ratio);
            }
        }
    }

    private void filterNonEntities(Annotations taggedEntities, String text) {
        //        List<String> tokens = Tokenizer.tokenize(text);
        //        Set<String> lowercaseTokens = CollectionHelper.filter(tokens, new Filter<String>() {
        //            @Override
        //            public boolean accept(String item) {
        //                return !StringHelper.startsUppercase(item);
        //            }
        //        }, new HashSet<String>());
        //        Iterator<Annotation> iterator = taggedEntities.iterator();
        //        while (iterator.hasNext()) {
        //            // FIXME only do this with entities which are at sentence start!
        //            Annotation current = iterator.next();
        //            if (lowercaseTokens.contains(current.getEntity().toLowerCase())) {
        //                iterator.remove();
        //                System.out.println("Remove lowercase entity " + current.getEntity());
        //            }
        //        }

        Map<String, String> result = EntityPreprocessor.correctAnnotations(text, CASE_DICTIONARY);
        Iterator<Annotation> iterator = taggedEntities.iterator();
        while (iterator.hasNext()) {
            Annotation current = iterator.next();
            String value = current.getEntity();
            String mapping = result.get(value);
            if (mapping == null) {
                continue;
            }
            if (mapping.isEmpty()) {
                iterator.remove();
            }
            int indexCorrector = value.indexOf(mapping);
            current.setOffset(current.getOffset() + indexCorrector);
            current.setEntity(mapping);
            current.setLength(mapping.length());
        }
    }

    /**
     * Check, if a Collection of {@link Location}s are "ambiguous". The condition of ambiguity is fulfilled, if two
     * given Locations in the Collection have a greater distance then a specified threshold.
     * 
     * @param locations
     * @return
     */
    private boolean checkAmbiguity(Collection<Location> locations) {
        if (locations.size() <= 1) {
            return false;
        }
        List<Location> temp = new ArrayList<Location>(locations);
        for (int i = 0; i < temp.size(); i++) {
            Location location1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                Location location2 = temp.get(j);
                double distance = getDistance(location1, location2);
                if (distance > 50) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final List<LocationType> TYPE_PRIORITY = CollectionHelper.newArrayList();

    static {
        TYPE_PRIORITY.add(LocationType.CONTINENT);
        TYPE_PRIORITY.add(LocationType.COUNTRY);
        TYPE_PRIORITY.add(LocationType.CITY);
        TYPE_PRIORITY.add(LocationType.UNIT);
        TYPE_PRIORITY.add(LocationType.LANDMARK);
        TYPE_PRIORITY.add(LocationType.POI);
        TYPE_PRIORITY.add(LocationType.REGION);
        TYPE_PRIORITY.add(LocationType.ZIP);
        TYPE_PRIORITY.add(LocationType.STREET);
        TYPE_PRIORITY.add(LocationType.STREETNR);
        TYPE_PRIORITY.add(LocationType.UNDETERMINED);
        // TYPE_PRIORITY.add(LocationType.CONTINENT);
        // TYPE_PRIORITY.add(LocationType.COUNTRY);
        // TYPE_PRIORITY.add(LocationType.CITY);
        // TYPE_PRIORITY.add(LocationType.UNIT);
        // TYPE_PRIORITY.add(LocationType.REGION);
        // TYPE_PRIORITY.add(LocationType.LANDMARK);
        // TYPE_PRIORITY.add(LocationType.POI);
        // TYPE_PRIORITY.add(LocationType.ZIP);
        // TYPE_PRIORITY.add(LocationType.STREET);
        // TYPE_PRIORITY.add(LocationType.STREETNR);
        // TYPE_PRIORITY.add(LocationType.UNDETERMINED);
    }

    /**
     * Select one location when multiple were retrieved. Currently simply rank by prior.
     * 
     * @param retrievedLocations
     * @return
     */
    private Location selectLocation(Collection<Location> retrievedLocations) {
        //        Collections.sort(retrievedLocations, new Comparator<Location>() {
        //            @Override
        //            public int compare(Location o1, Location o2) {
        //                Integer prio1 = TYPE_PRIORITY.indexOf(o1.getType());
        //                Integer prio2 = TYPE_PRIORITY.indexOf(o2.getType());
        //                return prio1.compareTo(prio2);
        //            }
        //        });
        List<Location> temp = new ArrayList<Location>(retrievedLocations);
        Collections.sort(temp, new Comparator<Location>() {
            @Override
            public int compare(Location l1, Location l2) {
                if (l1.getType() != l2.getType()) {
                    if (l1.getType() == LocationType.CONTINENT) {
                        return -1;
                    }
                    if (l2.getType() == LocationType.CONTINENT) {
                        return 1;
                    }
                }
                // if (l2.getType() == LocationType.UNIT) {
                // return -1;
                // }
                Long l1Population = l1.getPopulation();
                Long l2Population = l2.getPopulation();
                if (l1.getType() == LocationType.CITY) {
                    l1Population *= 2;
                }
                if (l2.getType() == LocationType.CITY) {
                    l2Population *= 2;
                }
                return l2Population.compareTo(l1Population);
            }
        });
        // Collections.sort(retrievedLocations, new Comparator<Location>() {
        // @Override
        // public int compare(Location l1, Location l2) {
        // int priority1 = TYPE_PRIORITY.indexOf(l1.getType());
        // int priority2 = TYPE_PRIORITY.indexOf(l2.getType());
        // return Integer.valueOf(priority1).compareTo(priority2);
        // }
        // });
        return CollectionHelper.getFirst(temp);
    }

    /**
     * <p>
     * Often we can find places with the same name at different locations. We need to find out which places are the most
     * likely.
     * </p>
     * <ol>
     * <li>First we replace all city entities if we also found a country entity with the same name.</li>
     * <li>If we have several cities with the same name we pick the one that is in a country of the list or if there is
     * no country, we pick the largest city.</li>
     * </ol>
     * 
     * @param locations The set of detected entity candidates.
     * @return A reduced set of entities containing only the most likely ones.
     */
    private List<Location> processCandidateList(List<Location> locations) {
        Set<Location> entitiesToRemove = new HashSet<Location>();

        Set<Location> countries = new HashSet<Location>();
        Set<Location> cities = new HashSet<Location>();
        Map<String, Set<Location>> citiesWithSameName = new HashMap<String, Set<Location>>();

        // STEP 1: if we have cities and countries with the same name, we remove the cities
        for (Location location : locations) {

            // check whether entity is a city and we have a country
            // boolean keepLocation = true;
            if (location.getType() == LocationType.CITY) {
                cities.add(location);
                if (citiesWithSameName.get(location.getPrimaryName()) != null) {
                    citiesWithSameName.get(location.getPrimaryName()).add(location);
                } else {
                    Set<Location> set = new HashSet<Location>();
                    set.add(location);
                    citiesWithSameName.put(location.getPrimaryName(), set);
                }

                for (Location entity2 : locations) {
                    if (entity2.getType() == LocationType.COUNTRY
                            && entity2.getPrimaryName().equalsIgnoreCase(location.getPrimaryName())) {
                        // keepLocation = false;
                        entitiesToRemove.add(location);
                    }
                }
            } else if (location.getType() == LocationType.COUNTRY) {
                countries.add(location);
            }

            // if (keepLocation) {
            // entitiesToRemove.add(location);
            // }
        }

        // STEP 2: if we have several cities with the same name, we pick the one in the country or the largest
        // for (Set<Location> citySet : citiesWithSameName.values()) {
        // // cities with the same name
        // if (citySet.size() > 1) {
        //
        // // calculate distance to all countries, take the city closest to any country
        // Location closestCity = null;
        // double closestDistance = Integer.MAX_VALUE;
        // Location biggestCity = null;
        // long biggestPopulation = -1;
        // for (Location city : citySet) {
        //
        // // update biggest city
        // long population = getPopulation(city);
        // if (population > biggestPopulation) {
        // biggestCity = city;
        // biggestPopulation = population;
        // }
        //
        // // upate closest city to countries
        // for (Location country : countries) {
        //
        // double distance = getDistance(city, country);
        // if (distance < closestDistance) {
        // closestCity = city;
        // closestDistance = distance;
        // }
        //
        // }
        // }
        //
        // // we keep only one city
        // Location keepCity = null;
        // if (closestCity != null) {
        // keepCity = closestCity;
        // } else if (biggestCity != null) {
        // keepCity = biggestCity;
        // } else {
        // keepCity = citySet.iterator().next();
        // }
        //
        // citySet.remove(keepCity);
        //
        // for (Location entity : citySet) {
        // entitiesToRemove.add(entity);
        // }
        // // entitiesToRemove.addAll(citySet);
        // }
        // }

        // CollectionHelper.print(entitiesToRemove);

        for (Location entity : entitiesToRemove) {
            locations.remove(entity);
        }
        // entitiesToRemove.addAll(removeLocationsOutOfBounds(locations));
        // for (Location entity : entitiesToRemove) {
        // locations.remove(entity);
        // }

        return locations;
    }

    /**
     * <p>
     * We remove locations that are not in one "bounding location". A bounding location is a country or continent.
     * </p>
     * 
     * @param locations The locations to check.
     * @return A collection of locations that should be removed.
     */
    private Collection<Location> removeLocationsOutOfBounds(Collection<Location> locations) {
        Collection<Location> toKeep = new HashSet<Location>();

        for (Location location : locations) {
            List<Location> hierarchy = locationSource.getHierarchy(location.getId());

            // check whether another location is in the hierarchy of this location
            for (Location hierarchyLocation : hierarchy) {
                if (hierarchyLocation.getType() == LocationType.COUNTRY) {
                    for (Location location2 : locations) {
                        if (location2.getType() == LocationType.COUNTRY
                                && location2.getPrimaryName().equalsIgnoreCase(hierarchyLocation.getPrimaryName())) {
                            toKeep.add(location);
                        }
                    }
                }
            }
        }

        locations.removeAll(toKeep);
        return locations;
    }

    private double getDistance(Location city, Location country) {
        double distance = Integer.MAX_VALUE;

        try {

            Double lat1 = city.getLatitude();
            Double lng1 = city.getLongitude();
            Double lat2 = country.getLatitude();
            Double lng2 = country.getLongitude();

            distance = MathHelper.computeDistanceBetweenWorldCoordinates(lat1, lng1, lat2, lng2);

        } catch (Exception e) {
        }

        return distance;
    }

    // FIXME -> not cool, NER learns that stuff and many more
    private static final List<String> PREFIXES = Arrays.asList("Mrs.", "Mrs", "Mr.", "Mr", "Ms.", "Ms", "President",
            "Minister", "General", "Sir", "Lady", "Democrat", "Republican", "Senator", "Chief", "Whip", "Reverend",
            "Detective", "Det", "Superintendent", "Supt", "Chancellor", "Cardinal", "Premier", "Representative",
            "Governor", "Minister", "Dr.", "Dr", "Professor", "Prof.", "Prof", "Lawyer", "Inspector", "Admiral",
            "Officer", "Cyclist", "Commissioner", "Olympian", "Sergeant", "Shareholder", "Coroner", "Constable",
            "Magistrate", "Judge", "Futurist", "Recorder", "Councillor", "Councilor", "King", "Reporter", "Leader",
            "Executive", "Justice", "Secretary", "Prince", "Congressman", "Skipper", "Liberal", "Analyst", "Major",
            "Writer", "Ombudsman", "Examiner");

    private void filterPersonEntities(Annotations annotations) {
        Set<String> blacklist = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            String value = annotation.getEntity().toLowerCase();
            for (String prefix : PREFIXES) {
                if (value.contains(prefix.toLowerCase() + " ")) {
                    blacklist.addAll(Arrays.asList(annotation.getEntity().toLowerCase().split("\\s")));
                }
                if (value.endsWith(" gmbh") || value.endsWith(" inc.") || value.endsWith(" co.")
                        || value.endsWith(" corp.")) {
                    blacklist.addAll(Arrays.asList(annotation.getEntity().toLowerCase().split("\\s")));
                }
            }
        }
        Iterator<Annotation> iterator = annotations.iterator();
        while (iterator.hasNext()) {
            Annotation annotation = iterator.next();
            String value = annotation.getEntity().toLowerCase();
            boolean remove = blacklist.contains(value);
            for (String blacklistedItem : blacklist) {
                if (StringHelper.containsWord(blacklistedItem, value)) {
                    remove = true;
                    break;
                }
            }
            if (remove) {
                LOGGER.debug("Remove " + annotation);
                iterator.remove();
            }
        }
    }

    @Override
    public String getName() {
        return "PalladianLocationExtractor";
    }

    static class LocationTypeFilter implements Filter<Location> {

        private final LocationType type;

        /**
         * @param type
         */
        public LocationTypeFilter(LocationType type) {
            this.type = type;
        }

        @Override
        public boolean accept(Location item) {
            return item.getType() == type;
        }

    }

    public static void main(String[] args) throws PageContentExtractorException {

        // System.out.println(makeAcronymSeparated("USA"));
        // System.exit(0);

        // String mashapePublicKey = "u3ewnlzvxvbg3gochzqcrulimgngsb";
        // String mashapePrivateKey = "dxkyimj8rjoyti1mqx2lqragbbg71k";
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        PalladianLocationExtractor extractor = new PalladianLocationExtractor(database);

        String rawText = FileHelper
                .readFileToString("/Users/pk/Desktop/LocationLab/LocationExtractionDataset/text46.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);

        // String cleanText = "Light";

        // Annotations taggedEntities = StringTagger.getTaggedEntities(cleanText);
        // CollectionHelper.print(taggedEntities);
        // filterNonLocations(taggedEntities);
        // CollectionHelper.print(taggedEntities);
        // System.exit(0);

        List<Annotation> locations = extractor.getAnnotations(cleanText);
        CollectionHelper.print(locations);

        // String text = "";
        //
        // PalladianContentExtractor pce = new PalladianContentExtractor();
        // text = pce.setDocument("http://www.bbc.co.uk/news/world-africa-17887914").getResultText();
        //
        // PalladianLocationExtractor locationDetector = new PalladianLocationExtractor();
        // Collection<Location> locations = locationDetector.detectLocations(text);
        //
        // CollectionHelper.print(locations);
    }

}
