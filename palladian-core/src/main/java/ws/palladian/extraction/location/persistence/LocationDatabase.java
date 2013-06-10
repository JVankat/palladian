package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.OneColumnRowConverter;
import ws.palladian.persistence.ResultSetCallback;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;

/**
 * <p>
 * A {@link LocationStore} which is realized by a SQL database. Use the {@link DatabaseManagerFactory} to create
 * instances of this class. The database schema can be found in <code>/config/locationDbSchema.sql</code>.
 * <b>Important:</b> To work correctly, the SQL database's group_concat_length must be set to a value of at least
 * {@value #EXPECTED_GROUP_CONCAT_LENGTH}; therefore the parameter
 * <code>sessionVariables=group_concat_max_len=1048576</code> has to be appended to the JDBC URL which is supplied to
 * the {@link DatabaseManagerFactory}.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public final class LocationDatabase extends DatabaseManager implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDatabase.class);

    /** The minimum 'group_concat_length' expected from the database. */
    private static final int EXPECTED_GROUP_CONCAT_LENGTH = 1024 * 1024;

    // ////////////////// location prepared statements ////////////////////
    private static final String ADD_LOCATION = "INSERT INTO locations SET id = ?, type = ?, name= ?, longitude = ?, latitude = ?, population = ?";
    private static final String ADD_ALTERNATIVE_NAME = "INSERT IGNORE INTO location_alternative_names SET locationId = ?, alternativeName = ?, language = ?";
    private static final String GET_LOCATIONS_LANGUAGE = "{call search_locations(?,?)}";
    private static final String GET_LOCATIONS_BY_ID = "SELECT l.*,lan.*,GROUP_CONCAT(alternativeName,'','#',IFNULL(language,'')) AS alternatives FROM locations l LEFT JOIN location_alternative_names lan ON l.id = lan.locationId WHERE l.id IN(%s) GROUP BY id;";
    private static final String ADD_HIERARCHY = "INSERT INTO locations SET id = ?, ancestorIds = ?, type = '', name = '' ON DUPLICATE KEY UPDATE ancestorIds = ?";
    private static final String GET_ANCESTOR_IDS = "SELECT ancestorIds FROM locations WHERE id = ?";
    private static final String UPDATE_HIERARCHY = "UPDATE locations SET ancestorIds = CONCAT(?, ancestorIds) WHERE ancestorIds LIKE ?";
    private static final String GET_HIGHEST_LOCATION_ID = "SELECT MAX(id) FROM locations";
    private static final String GET_LOCATIONS_BY_COORDINATE = "SELECT l.*, lan.*, GROUP_CONCAT(alternativeName,'','#',IFNULL(language,'')) AS alternatives, 6371 * 2 * ASIN(SQRT(POWER(SIN(RADIANS(? - ABS(latitude))), 2) + COS(RADIANS(?)) * COS(RADIANS(ABS(latitude))) * POWER(SIN(RADIANS(? - longitude)), 2))) AS distance FROM locations l LEFT JOIN location_alternative_names lan ON l.id = lan.locationId WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ? GROUP BY id HAVING distance < ? ORDER BY distance;";

    // ////////////////// row converts ////////////////////////////////////
    private static final RowConverter<Location> LOCATION_CONVERTER = new RowConverter<Location>() {
        @Override
        public Location convert(ResultSet resultSet) throws SQLException {
            int id = resultSet.getInt("id");
            LocationType locationType = LocationType.map(resultSet.getString("type"));
            String name = resultSet.getString("name");

            List<AlternativeName> altNames = CollectionHelper.newArrayList();
            String alternativesString = resultSet.getString("alternatives");
            if (alternativesString != null) {
                for (String nameLanguageString : alternativesString.split(",")) {
                    String[] parts = nameLanguageString.split("#");
                    if (parts[0].equalsIgnoreCase("alternativeName")) {
                        continue;
                    }
                    Language language = null;
                    if (parts.length > 1) {
                        language = Language.getByIso6391(parts[1]);
                    }
                    altNames.add(new AlternativeName(parts[0], language));
                }
            }

            Double latitude = SqlHelper.getDouble(resultSet, "latitude");
            Double longitude = SqlHelper.getDouble(resultSet, "longitude");
            Long population = resultSet.getLong("population");
            List<Integer> ancestorIds = splitHierarchyPath(SqlHelper.getString(resultSet, "ancestorIds"));
            return new ImmutableLocation(id, name, altNames, locationType, latitude, longitude, population, ancestorIds);
        }
    };

    // //////////////////////////////////////////////////////////////////////

    /** Instances are created using the {@link DatabaseManagerFactory}. */
    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
        checkGroupConcatLength();
    }

    /**
     * Check the configured value for the 'group_concat_max_len', and throw an {@link IllegalStateException} if value is
     * too small.
     */
    private final void checkGroupConcatLength() {
        runQuery(new ResultSetCallback() {
            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                int groupConcatLength = resultSet.getInt(2);
                if (groupConcatLength < EXPECTED_GROUP_CONCAT_LENGTH) {
                    throw new IllegalStateException(
                            "Please increase 'group_concat_max_len'; it is currently set to "
                                    + groupConcatLength
                                    + ", but should be at least "
                                    + EXPECTED_GROUP_CONCAT_LENGTH
                                    + " for the LocationDatabase to work correctly. See the class documentation for more information.");
                }
            }
        }, "SHOW SESSION VARIABLES LIKE 'group_concat_max_len'");
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        return getLocations(Collections.singletonList(locationName), languages).get(locationName);
    }

    /**
     * <p>
     * Create a parameter mask for dynamically creating prepared statements. Example of a result looks like "?,?,?,?".
     * </p>
     * 
     * @param numParams The number of parameters in the mask.
     * @return
     */
    private static final String createMask(int numParams) {
        return StringUtils.repeat("?", ",", numParams);
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        final MultiMap<String, Location> result = DefaultMultiMap.createWithSet();
        if (locationNames.isEmpty()) {
            return result;
        }
        StringBuilder languageList = new StringBuilder();
        boolean first = true;
        for (Language language : languages) {
            if (first) {
                first = false;
            } else {
                languageList.append(",");
            }
            languageList.append(language.getIso6391());
        }
        String names = StringUtils.join(locationNames, ',');
        runQuery(new ResultSetCallback() {
            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                String query = resultSet.getString("query");
                result.add(query, LOCATION_CONVERTER.convert(resultSet));
            }
        }, GET_LOCATIONS_LANGUAGE, names, languageList.toString());
        return result;
    }

    @Override
    public Location getLocation(int locationId) {
        return CollectionHelper.getFirst(getLocations(Collections.singletonList(locationId)));
    }

    @Override
    public List<Location> getLocations(final List<Integer> locationIds) {
        Validate.notNull(locationIds, "locationIds must not be null");
        if (locationIds.isEmpty()) {
            return Collections.emptyList();
        }

        // the Prepared Statement needs to be re-compiled for every unique number of locationIds we have to search.
        // This might be an issue, but usually there should not be too many different counts (1-10, I suspect), so that
        // all used combinations will get and stay cached eventually.

        String prepStmt = String.format(GET_LOCATIONS_BY_ID, createMask(locationIds.size()));
        List<Location> locations = runQuery(LOCATION_CONVERTER, prepStmt, locationIds);

        // sort the returned list, so that we have the order of the given locations IDs
        Collections.sort(locations, new Comparator<Location>() {
            @Override
            public int compare(Location l0, Location l1) {
                return locationIds.indexOf(l0.getId()) - locationIds.indexOf(l1.getId());
            }
        });

        return locations;
    }

    @Override
    public void save(Location location) {
        List<Object> args = CollectionHelper.newArrayList();
        args.add(location.getId());
        args.add(location.getType().toString());
        args.add(location.getPrimaryName());
        args.add(location.getLongitude());
        args.add(location.getLatitude());
        args.add(location.getPopulation());
        int generatedLocationId = runInsertReturnId(ADD_LOCATION, args);

        if (generatedLocationId < 1) {
            // TODO something went wrong
            return;
        }

        // save alternative location names
        if (location.getAlternativeNames() != null) {
            addAlternativeNames(generatedLocationId, location.getAlternativeNames());
        }
    }

    @Override
    public void addHierarchy(int childId, int parentId) {
        String parentAncestorPath = runSingleQuery(OneColumnRowConverter.STRING, GET_ANCESTOR_IDS, parentId);
        String ancestorPath = (parentAncestorPath != null ? parentAncestorPath : "/") + parentId;
        String addAncestorPath = ancestorPath + "/";
        runUpdate(ADD_HIERARCHY, childId, addAncestorPath, addAncestorPath);
        runUpdate(UPDATE_HIERARCHY, ancestorPath, "/" + childId + "/%");
    }

    /**
     * <p>
     * Split up an hierarchy path into single IDs. An hierarchy path looks like
     * "/6295630/6255148/2921044/2951839/2861322/3220837/6559171/" and is used to flatten the hierarchy relation in the
     * database into one column per entry. In the database, to root node is at the beginning of the string; this method
     * does a reverse ordering, so that result contains the root node as last element.
     * </p>
     * 
     * @param hierarchyPath The hierarchy path.
     * @return List with IDs, in reverse order. Empty {@link List}, if hierarchy path was <code>null</code> or empty.
     */
    private static final List<Integer> splitHierarchyPath(String hierarchyPath) {
        if (hierarchyPath == null) {
            return Collections.emptyList();
        }
        List<Integer> ancestorIds = CollectionHelper.newArrayList();
        String[] splitPath = hierarchyPath.split("/");
        for (int i = splitPath.length - 1; i >= 0; i--) {
            String ancestorId = splitPath[i];
            if (StringUtils.isNotBlank(ancestorId)) {
                ancestorIds.add(Integer.valueOf(ancestorId));
            }
        }
        return ancestorIds;
    }

    /**
     * <p>
     * Delete the content in the location database.
     * </p>
     */
    public void truncate() {
        System.out.println("Really truncate the location database?");
        new Scanner(System.in).nextLine();

        LOGGER.warn("Truncating the database");

        runUpdate("TRUNCATE TABLE locations");
        runUpdate("TRUNCATE TABLE location_alternative_names");
    }

    /**
     * <p>
     * Flush tables and reset query cache for performance checks.
     * </p>
     */
    public void resetForPerformanceCheck() {
        runUpdate("FLUSH TABLES");
        runUpdate("RESET QUERY CACHE");
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        for (AlternativeName alternativeName : alternativeNames) {
            String languageString = StringUtils.EMPTY;
            if (alternativeName.getLanguage() != null) {
                languageString = alternativeName.getLanguage().getIso6391();
            }
            runInsertReturnId(ADD_ALTERNATIVE_NAME, locationId, alternativeName.getName(), languageString);
        }
    }

    @Override
    public int getHighestId() {
        Integer id = runSingleQuery(OneColumnRowConverter.INTEGER, GET_HIGHEST_LOCATION_ID);
        return id != null ? id : 0;
    }

    // @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        // see http://vinsol.com/blog/2011/08/30/geoproximity-search-with-mysql/
        double[] box = GeoUtils.getBoundingBox(coordinate, distance);
        Double lat = coordinate.getLatitude();
        Double lng = coordinate.getLongitude();
        Object[] args = new Object[] {lat, lat, lng, box[0], box[2], box[1], box[3], distance};
        return runQuery(LOCATION_CONVERTER, GET_LOCATIONS_BY_COORDINATE, args);
    }

    public static void main(String[] args) {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        List<Location> locations = database.getLocations(new ImmutableGeoCoordinate(49.1, 9.216667), 10);
        CollectionHelper.print(locations);
    }

}
