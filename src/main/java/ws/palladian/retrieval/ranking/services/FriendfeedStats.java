package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for posts containing a given url on FriendFeed. Only entries with the service id
 * "internal" are not counted, these are FriendFeed posts.
 * </p>
 * <p>
 * No specifics on rate limiting.
 * </p>
 * 
 * @author Julien Schmehl
 * @see http://www.friendfeed.com/
 * @see http://friendfeed.com/api/services
 */
public class FriendfeedStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(FriendfeedStats.class);

    private static final String GET_ENTRIES = "http://friendfeed.com/api/feed/url?url=";

    /** The id of this service. */
    private static final String SERVICE_ID = "friendfeed";

    /** The ranking value types of this service **/
    static RankingType POSTS = new RankingType("friendfeed_int_posts", "FriendFeed posts",
            "The number of entries posted on FriendFeed containing the given url.");
    static RankingType LIKES = new RankingType("friendfeed_int_likes", "FriendFeed likes",
            "The number of likes for entries posted on FriendFeed containing the given url.");
    static RankingType COMMENTS = new RankingType("friendfeed_int_comments", "FriendFeed comments",
            "The number of comments for entries posted on FriendFeed containing the given url.");
    static List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(POSTS);
        RANKING_TYPES.add(LIKES);
        RANKING_TYPES.add(COMMENTS);
    }

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public FriendfeedStats() {
        super();
    }

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {
            String encUrl = UrlHelper.urlEncode(url);
            JSONObject json = retriever.getJSONDocument(GET_ENTRIES + encUrl);
            if (json != null) {
                JSONArray entries = json.getJSONArray("entries");
                float posts = 0;
                float likes = 0;
                float comments = 0;
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject post = entries.getJSONObject(i);
                    if (post.getJSONObject("service").getString("id").equals("internal")) {
                        posts++;
                        likes += post.getJSONArray("likes").length();
                        comments += post.getJSONArray("comments").length();
                    }
                }
                results.put(POSTS, posts);
                results.put(LIKES, likes);
                results.put(COMMENTS, comments);
                LOGGER.trace("FriendFeed stats for " + url + " : " + results);
            } else {
                results.put(POSTS, null);
                results.put(LIKES, null);
                results.put(COMMENTS, null);
                LOGGER.trace("FriendFeed stats for " + url + "could not be fetched");
                checkBlocked();
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        }
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        boolean error = false;
        try {
            JSONObject json = retriever.getJSONDocument(GET_ENTRIES + "http://www.google.com/");
            if (json != null) {
                if (json.has("errorCode")) {
                    if (json.get("errorCode").equals("limit-exceeded")) {
                        error = true;
                    }
                }
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        }
        if (!error) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("FriendFeed Ranking Service is momentarily blocked. Will check again in 1min.");
        return true;
    }

    @Override
    public boolean isBlocked() {
        if (new Date().getTime() - lastCheckBlocked < checkBlockedIntervall) {
            return blocked;
        } else {
            return checkBlocked();
        }
    }

    @Override
    public void resetBlocked() {
        blocked = false;
        lastCheckBlocked = new Date().getTime();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

}
