package ws.palladian.extraction.location;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Immutable default implementation of a {@link Location}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class ImmutableLocation extends AbstractLocation {

    private final int id;
    private final String primaryName;
    private final Collection<AlternativeName> alternativeNames;
    private final LocationType type;
    private final GeoCoordinate coordinate;
    private final Long population;
    private final List<Integer> ancestorIds;
    private final Map<String, Object> metaData;

    /**
     * <p>
     * Create a new location with the specified attributes.
     * </p>
     * 
     * @param id The unique identifier of the location.
     * @param primaryName The primary name of the location, not <code>null</code>.
     * @param alternativeNames A list of potential alternative names for the location, may be <code>null</code>, if no
     *            alternative names exist.
     * @param type The type of the location, not <code>null</code>.
     * @param coordinate The geographical coordinate, or <code>null</code> if no coordinates exist.
     * @param population The population, or <code>null</code> if no population values exist.
     * @param ancestorIds The IDs of ancestor {@link ImmutableLocation}s, or <code>null</code> if no ancestors exist.
     */
    public ImmutableLocation(int id, String primaryName, Collection<AlternativeName> alternativeNames,
            LocationType type, GeoCoordinate coordinate, Long population, List<Integer> ancestorIds, Map<String, Object> metaData) {
        Validate.notNull(primaryName, "primaryName must not be null");
        Validate.notNull(type, "type must not be null");
        this.id = id;
        this.primaryName = primaryName;
        this.alternativeNames = alternativeNames != null ? alternativeNames : Collections.emptyList();
        this.type = type;
        this.coordinate = coordinate;
        this.population = population;
        this.ancestorIds = ancestorIds != null ? ancestorIds : Collections.emptyList();
        this.metaData = metaData;
    }

    /**
     * <p>
     * Create a new location with the specified attributes.
     * </p>
     *
     * @param id The unique identifier of the location.
     * @param primaryName The primary name of the location, not <code>null</code>.
     * @param alternativeNames A list of potential alternative names for the location, may be <code>null</code>, if no
     *            alternative names exist.
     * @param type The type of the location, not <code>null</code>.
     * @param coordinate The geographical coordinate, or <code>null</code> if no coordinates exist.
     * @param population The population, or <code>null</code> if no population values exist.
     * @param ancestorIds The IDs of ancestor {@link ImmutableLocation}s, or <code>null</code> if no ancestors exist.
     */
    public ImmutableLocation(int id, String primaryName, Collection<AlternativeName> alternativeNames,
            LocationType type, GeoCoordinate coordinate, Long population, List<Integer> ancestorIds) {
        this(id, primaryName, alternativeNames, type, coordinate, population, ancestorIds, null);
    }

    /**
     * <p>
     * Create a new location with the specified attributes.
     * </p>
     * 
     * @param id The unique identifier of the location.
     * @param primaryName The primary name of the location, not <code>null</code>.
     * @param type The type of the location, not <code>null</code>.
     * @param coordinate The geographical coordinate, or <code>null</code> if no coordinates exist.
     * @param population The population, or <code>null</code> if no population values exist.
     */
    public ImmutableLocation(int id, String primaryName, LocationType type, GeoCoordinate coordinate, Long population) {
        this(id, primaryName, null, type, coordinate, population, null);
    }

    /**
     * <p>
     * Create a new location with the specified attributes.
     * </p>
     *
     * @param id The unique identifier of the location.
     * @param primaryName The primary name of the location, not <code>null</code>.
     * @param type The type of the location, not <code>null</code>.
     * @param coordinate The geographical coordinate, or <code>null</code> if no coordinates exist.
     * @param population The population, or <code>null</code> if no population values exist.
     * @param metaData Meta data of the location.
     */
    public ImmutableLocation(int id, String primaryName, LocationType type, GeoCoordinate coordinate, Long population, Map<String, Object> metaData) {
        this(id, primaryName, null, type, coordinate, population, null,metaData);
    }

    /**
     * <p>
     * Copy an existing {@link Location} and add alternative names and ancestor IDs.
     * </p>
     * 
     * @param location The {@link Location} for which to create a copy, not <code>null</code>.
     * @param alternativeNames A list of potential alternative names for the location, may be <code>null</code>, if no
     *            alternative names exist.
     * @param ancestorIds The IDs of ancestor {@link ImmutableLocation}s, or <code>null</code> if no ancestors exist.
     */
    public ImmutableLocation(Location location, Collection<AlternativeName> alternativeNames, List<Integer> ancestorIds) {
        this(location.getId(), location.getPrimaryName(), alternativeNames, location.getType(), location
                .getCoordinate(), location.getPopulation(), ancestorIds);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getPrimaryName() {
        return primaryName;
    }

    @Override
    public Collection<AlternativeName> getAlternativeNames() {
        return Collections.unmodifiableCollection(alternativeNames);
    }
    @Override
    public Collection<String> getAlternativeNameStrings() {
        if (alternativeNames == null) {
            return new HashSet<>();
        }
        return alternativeNames.stream().map(AlternativeName::getName).collect(Collectors.toSet());
    }

    @Override
    public LocationType getType() {
        return type;
    }

    @Override
    public Long getPopulation() {
        return population;
    }

    @Override
    public List<Integer> getAncestorIds() {
        return Collections.unmodifiableList(ancestorIds);
    }

    @Override
    public GeoCoordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public Map<String, Object> getMetaData() {
        return metaData;
    }
}
