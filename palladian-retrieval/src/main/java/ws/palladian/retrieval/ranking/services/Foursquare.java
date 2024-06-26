package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * {@link RankingService} implementation to find the number of checkins and likes for a location on <a
 * href="https://foursquare.com>foursquare</a>. <b>Important:</b> This does not conform to the other ranking
 * implementations as it does not reveive URLs, but vanue IDs as parameter.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://developer.foursquare.com">foursquare for Developers</a>
 */
public final class Foursquare extends AbstractRankingService implements RankingService {

    /** {@link Configuration} key for the client id. */
    public static final String CONFIG_CLIENT_ID = "api.foursquare.clientId";

    /** {@link Configuration} key for the client secret. */
    public static final String CONFIG_CLIENT_SECRET = "api.foursquare.clientSecret";

    /** The id of this service. */
    private static final String SERVICE_ID = "foursquare";

    /** The ranking value types of this service **/
    public static final RankingType<Integer> FOURSQUARE_CHECKINS = new RankingType<>("checkins", "Foursquare Checkins", "The number of foursquare checkins of the location.", Integer.class);

    public static final RankingType<Integer> FOURSQUARE_LIKES = new RankingType<>("likes", "Foursquare Likes", "The number of foursquare likes of the location.", Integer.class);

    /** All available ranking types by {@link Foursquare}. */
    private static final List<RankingType<?>> RANKING_TYPES = Arrays.asList(FOURSQUARE_CHECKINS, FOURSQUARE_LIKES);

    private final String clientId;

    private final String clientSecret;

    /**
     * <p>
     * Create a new {@link Foursquare} ranking service.
     * </p>
     *
     * @param configuration The configuration which must provide {@value #CONFIG_CLIENT_ID} and
     *                      {@value #CONFIG_CLIENT_SECRET} for accessing the service.
     */
    public Foursquare(Configuration configuration) {
        this(configuration.getString(CONFIG_CLIENT_ID), configuration.getString(CONFIG_CLIENT_SECRET));
    }

    /**
     * <p>
     * Create a new {@link Foursquare} ranking service.
     * </p>
     *
     * @param clientId     The required client key for accessing the service, not <code>null</code> or empty.
     * @param clientSecret The required client secret for accessing the service, not <code>null</code> or empty.
     */
    public Foursquare(String clientId, String clientSecret) {
        Validate.notEmpty(clientId);
        Validate.notEmpty(clientSecret);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public Ranking getRanking(String venueId) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, venueId);

        int checkins = 0;
        int likes = 0;
        String requestUrl = buildRequestUrl(venueId);

        try {

            HttpResult httpGet = retriever.httpGet(requestUrl);
            JsonObject json = new JsonObject(httpGet.getStringContent());

            JsonObject venue = json.queryJsonObject("response/venue");
            checkins = venue.queryInt("stats/checkinsCount");
            likes = venue.queryInt("likes/count");

        } catch (Exception e) {
            throw new RankingServiceException(e);
        }

        builder.add(FOURSQUARE_CHECKINS, checkins);
        builder.add(FOURSQUARE_LIKES, likes);
        return builder.create();
    }

    /**
     * <p>
     * Build the request URL.
     * </p>
     *
     * @param venueId The id of the venue to search for.
     * @return The request URL.
     */
    private String buildRequestUrl(String venueId) {
        return String.format("https://api.foursquare.com/v2/venues/%s?v=20120321&client_id=%s&client_secret=%s", venueId, clientId, clientSecret);
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType<?>> getRankingTypes() {
        return RANKING_TYPES;
    }

}
