package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.ConnectionManager;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.ResultIterator;
import ws.palladian.persistence.ResultSetCallback;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;

/**
 * <p>The FeedDatabase is an implementation of the FeedStore that stores feeds and items in a relational database.</p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
public class FeedDatabase extends DatabaseManager implements FeedStore {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private static final String ADD_FEED_ITEM = "INSERT IGNORE INTO feed_items SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, authors = ?, description = ?, text = ?";
    private static final String ADD_FEED = "INSERT IGNORE INTO feeds SET feedUrl = ?, checks = ?, checkInterval = ?, newestItemHash = ?, unreachableCount = ?, unparsableCount = ?, lastFeedEntry = ?, activityPattern = ?, lastPollTime = ?, lastETag = ?, lastModified = ?, lastResult = ?, totalProcessingTime = ?, misses = ?, lastMissTimestamp = ?, blocked = ?, lastSuccessfulCheck = ?, windowSize = ?, hasVariableWindowSize = ?, totalItems = ?";
    private static final String UPDATE_FEED = "UPDATE feeds SET feedUrl = ?, checks = ?, checkInterval = ?, newestItemHash = ?, unreachableCount = ?, unparsableCount = ?, lastFeedEntry = ?, lastEtag = ?, lastModified = ?, lastResult = ?, lastPollTime = ?, activityPattern = ?, totalProcessingTime = ?, misses = ?, lastMissTimestamp = ?, blocked = ?, lastSuccessfulCheck = ?, windowSize = ?, hasVariableWindowSize = ?, totalItems = ? WHERE id = ?";
    private static final String UPDATE_FEED_POST_DISTRIBUTION = "REPLACE INTO feeds_post_distribution SET feedID = ?, minuteOfDay = ?, posts = ?, chances = ?";
    private static final String GET_FEED_POST_DISTRIBUTION = "SELECT minuteOfDay, posts, chances FROM feeds_post_distribution WHERE feedID = ?";
    private static final String GET_FEEDS = "SELECT * FROM feeds"; // ORDER BY id ASC";
    private static final String GET_FEED_BY_URL = "SELECT * FROM feeds WHERE feedUrl = ?";
    private static final String GET_FEED_BY_ID = "SELECT * FROM feeds WHERE id = ?";
    private static final String GET_ITEMS_BY_RAW_ID = "SELECT * FROM feed_items WHERE rawID = ?";
    private static final String GET_ITEMS_BY_RAW_ID_2 = "SELECT * FROM feed_items WHERE feedId = ? AND rawID = ?";
    private static final String CHANGE_CHECK_APPROACH = "UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, newestItemHash = '', checks = 0, lastFeedEntry = NULL";
    private static final String GET_ITEMS = "SELECT * FROM feed_items LIMIT ? OFFSET ?";
    private static final String GET_ALL_ITEMS = "SELECT * FROM feed_items";
    private static final String GET_ITEM_BY_ID = "SELECT * FROM feed_items WHERE id = ?";
    private static final String DELETE_ITEM_BY_ID = "DELETE FROM feed_items WHERE id = ?";
    private static final String UPDATE_FEED_META_INFORMATION = "UPDATE feeds SET  siteUrl = ?, added = ?, title = ?, language = ?, feedSize = ?, httpHeaderSize = ?, supportsPubSubHubBub = ?, isAccessibleFeed = ?, feedFormat = ?, hasItemIds = ?, hasPubDate = ?, hasCloud = ?, ttl = ?, hasSkipHours = ?, hasSkipDays = ?, hasUpdated = ?, hasPublished = ? WHERE id = ?";
    private static final String GET_FEED_POLL_BY_ID_TIMESTAMP = "SELECT * FROM feed_polls WHERE id = ? AND pollTimestamp = ?";
    private static final String GET_FEED_POLLS_BY_ID = "SELECT * FROM feed_polls WHERE id = ?";
    private static final String GET_NEXT_FEED_POLL_BY_ID_AND_TIME = "SELECT * FROM feed_polls WHERE id = ? AND pollTimestamp >= ? ORDER BY pollTimestamp ASC LIMIT 0,1";
    private static final String ADD_FEED_POLL = "INSERT IGNORE INTO feed_polls SET id = ?, pollTimestamp = ?, httpETag = ?, httpDate = ?, httpLastModified = ?, httpExpires = ?, newestItemTimestamp = ?, numberNewItems = ?, windowSize = ?, httpStatusCode = ?, responseSize = ?";
    private static final String UPDATE_FEED_POLL = "UPDATE feed_polls SET httpETag = ?, httpDate = ?, httpLastModified = ?, httpExpires = ?, newestItemTimestamp = ?, numberNewItems = ?, windowSize = ?, httpStatusCode = ?, responseSize = ? WHERE id = ? AND pollTimestamp = ?";
    private static final String ADD_CACHE_ITEMS = "INSERT IGNORE INTO feed_item_cache SET id = ?, itemHash = ?, correctedPollTime = ?";
    private static final String GET_CACHE_ITEMS_BY_ID = "SELECT * FROM feed_item_cache WHERE id = ?";
    private static final String DELETE_CACHE_ITEMS_BY_ID = "DELETE FROM feed_item_cache WHERE id = ?";
    
    
    /**
     * @param connectionManager
     */
    protected FeedDatabase(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    /**
     * Truncate a string to 255 chars to store it as varchar(255). Additionally, control characters are removed.
     * In case the string is truncated, a message is written to error log.
     * 
     * @param input The string to truncate.
     * @param name The name of the input like "title" or "feedUrl", required to write meaningful log message.
     * @param feed Something to identify the feed. Use id or feedUrl. Required to write meaningful log message.
     * @return The input string, truncated to 255 chars if longer. <code>null</code> if input was <code>null</code>.
     */
    private String truncateToVarchar255(String input, String name, String feed) {
        String output = input;
        if (input != null) {
            output = StringHelper.removeControlCharacters(output);
            if (output.length() > 255) {
                output = output.substring(0, 255);
                LOGGER.error("Truncated " + name + " of feed " + feed + " to fit database. Original value was: "
                        + input);
            }
        }
        return output;
    }

    /**
     * Adds a feed and its meta information. The item cache is <b>not</b> not serialized!
     * 
     * @return <code>true</code> if feed and meta information have been added, <code>false</code> if at least one of
     *         feed or meta information have not been added.
     */
    @Override
    public boolean addFeed(Feed feed) {
        boolean added = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(truncateToVarchar255(feed.getFeedUrl(), "feedUrl", feed.getFeedUrl()));
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getNewestItemHash());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getUnparsableCount());
        parameters.add(feed.getLastFeedEntrySQLTimestamp());
        parameters.add(feed.getActivityPattern());
        parameters.add(feed.getLastPollTimeSQLTimestamp());
        parameters.add(truncateToVarchar255(feed.getLastETag(), "lastETag", feed.getFeedUrl()));
        parameters.add(feed.getHttpLastModifiedSQLTimestamp());
        parameters.add(feed.getLastFeedTaskResult());
        parameters.add(feed.getTotalProcessingTime());
        parameters.add(feed.getMisses());
        parameters.add(feed.getLastMissTime());
        parameters.add(feed.isBlocked());
        parameters.add(feed.getLastSuccessfulCheckTime());
        parameters.add(feed.getWindowSize());
        parameters.add(feed.hasVariableWindowSize());
        parameters.add(feed.getNumberOfItemsReceived());

        int result = runInsertReturnId(ADD_FEED, parameters);
        if (result > 0) {
            feed.setId(result);
            added = true;
        }

        if (added) {
            added = updateMetaInformation(feed);
            if (!added) {
                LOGGER.error("Feed id " + feed.getId() + " has been added but meta information could not be written!");
            }
        }

        return added;
    }

    @Override
    public boolean addFeedItem(Feed feed, FeedItem entry) {
        boolean added = false;

        int result = runInsertReturnId(ADD_FEED_ITEM, getItemParameters(feed, entry));
        if (result > 0) {
            entry.setId(result);
            added = true;
        }

        return added;
    }

    @Override
    public int addFeedItems(Feed feed, List<FeedItem> feedItems) {
        int added = 0;

        List<List<Object>> batchArgs = new ArrayList<List<Object>>();
        for (FeedItem feedItem : feedItems) {
            List<Object> parameters = getItemParameters(feed, feedItem);
            batchArgs.add(parameters);
        }

        // set the generated IDs back to the FeedItems and count number of added items
        int[] result = runBatchInsertReturnIds(ADD_FEED_ITEM, batchArgs);
        for (int i = 0; i < result.length; i++) {
            int id = result[i];
            if (id > 0) {
                feedItems.get(i).setId(id);
                added++;
            }
        }

        return added;
    }

    /**
     * When the check approach is switched we need to reset learned and calculated values such as check intervals,
     * checks, lastHeadlines etc.
     */
    public void changeCheckApproach() {
        runUpdate(CHANGE_CHECK_APPROACH);
    }

    public void clearFeedTables() {
        runUpdate("TRUNCATE TABLE feeds");
        runUpdate("TRUNCATE TABLE feed_items");
        runUpdate("TRUNCATE TABLE feeds_post_distribution");
        runUpdate("TRUNCATE TABLE feed_evaluation_polls");
    }

    public void deleteFeedItemById(int id) {
        runUpdate(DELETE_ITEM_BY_ID, id);
    }

    @Override
    public Feed getFeedByID(int feedID) {
        return runSingleQuery(new FeedRowConverter(), GET_FEED_BY_ID, feedID);
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        return runSingleQuery(new FeedRowConverter(), GET_FEED_BY_URL, feedUrl);
    }

    public FeedItem getFeedItemById(int id) {
        return runSingleQuery(new FeedItemRowConverter(), GET_ITEM_BY_ID, id);
    }

    @Override
    public FeedItem getFeedItemByRawId(int feedId, String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), GET_ITEMS_BY_RAW_ID_2, feedId, rawId);
    }

    @Deprecated
    public FeedItem getFeedItemByRawId(String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), GET_ITEMS_BY_RAW_ID, rawId);
    }

    public ResultIterator<FeedItem> getFeedItems() {
        return runQueryWithIterator(new FeedItemRowConverter(), GET_ALL_ITEMS);
    }

    /**
     * Get the specified count of feed items, starting at offset.
     * 
     * @param limit
     * @param offset
     * @return
     */
    public List<FeedItem> getFeedItems(int limit, int offset) {
        return runQuery(new FeedItemRowConverter(), GET_ITEMS, limit, offset);
    }

    /**
     * Get FeedItems by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_items table.
     * 
     * @param sqlQuery
     * @return
     */
    @Override
    public List<FeedItem> getFeedItemsBySqlQuery(String sqlQuery) {
        return runQuery(new FeedItemRowConverter(), sqlQuery);
    }

    public Map<Integer, int[]> getFeedPostDistribution(Feed feed) {

        final Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                int minuteOfDay = resultSet.getInt("minuteOfDay");
                int posts = resultSet.getInt("posts");
                int chances = resultSet.getInt("chances");
                int[] postsChances = { posts, chances };
                postDistribution.put(minuteOfDay, postsChances);
            }
        };

        runQuery(callback, GET_FEED_POST_DISTRIBUTION, feed.getId());
        return postDistribution;
    }

    @Override
    public List<Feed> getFeeds() {
        List<Feed> feeds = runQuery(new FeedRowConverter(), GET_FEEDS);
        for (Feed feed : feeds) {
            feed.setCachedItems(getCachedItemsById(feed.getId()));
        }
        return feeds;
    }

    private List<Object> getItemParameters(Feed feed, FeedItem entry) {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getId());
        parameters.add(entry.getTitle());
        parameters.add(entry.getLink());
        parameters.add(entry.getRawId());
        parameters.add(entry.getPublishedSQLTimestamp());
        parameters.add(entry.getAuthors());
        parameters.add(entry.getDescription());
        parameters.add(entry.getText());
        return parameters;
    }

    /**
     * Update feed in database.
     * 
     * @param feed The feed to update
     * @param updateMetaInformation If <code>true</code>, the feed'd meta information are updated.
     * @param replaceCachedItems Of <code>true</code>, the cached items are replaced by the ones contained in the feed.
     * @return <code>true</code> if (all) update(s) successful.
     */
    @Override
    public boolean updateFeed(Feed feed, boolean updateMetaInformation, boolean replaceCachedItems) {

        if (feed.getId() == -1) {
            LOGGER.debug("feed does not exist and is added therefore");
            return addFeed(feed);
        }

        boolean updated = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(truncateToVarchar255(feed.getFeedUrl(), "feedUrl", feed.getId() + ""));
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getNewestItemHash());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getUnparsableCount());
        parameters.add(feed.getLastFeedEntry());
        parameters.add(truncateToVarchar255(feed.getLastETag(), "lastETag", feed.getId() + ""));
        parameters.add(feed.getHttpLastModifiedSQLTimestamp());
        if (feed.getLastFeedTaskResult() != null) {
            parameters.add(feed.getLastFeedTaskResult().toString());
        } else {
            parameters.add(null);
        }
        parameters.add(feed.getLastPollTime());
        parameters.add(feed.getActivityPattern());
        parameters.add(feed.getTotalProcessingTime());
        parameters.add(feed.getMisses());
        parameters.add(feed.getLastMissTime());
        parameters.add(feed.isBlocked());
        parameters.add(feed.getLastSuccessfulCheckTime());
        parameters.add(feed.getWindowSize());
        parameters.add(feed.hasVariableWindowSize());
        parameters.add(feed.getNumberOfItemsReceived());
        parameters.add(feed.getId());

        updated = runUpdate(UPDATE_FEED, parameters) != -1;

        if (updated && updateMetaInformation) {
            updated = updateMetaInformation(feed);
            if (!updated) {
                LOGGER.error("Updating meta information for feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") failed.");
            }
        }

        if (updated && replaceCachedItems) {
            updated = deleteCachedItemById(feed.getId());
            if (!updated) {
                LOGGER.error("Deleting cached items for feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") failed.");
            }
            if (updated) {
                updated = addCacheItems(feed);
                if (!updated) {
                    LOGGER.error("Adding new cached items for feed id " + feed.getId() + " (" + feed.getFeedUrl()
                            + ") failed.");
                }
            }
        }

        return updated;
    }


    @Override
    public boolean updateFeed(Feed feed) {
        return updateFeed(feed, true, true);
    }

    public void updateFeedPostDistribution(Feed feed, Map<Integer, int[]> postDistribution) {
        for (java.util.Map.Entry<Integer, int[]> distributionEntry : postDistribution.entrySet()) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feed.getId());
            parameters.add(distributionEntry.getKey());
            parameters.add(distributionEntry.getValue()[0]);
            parameters.add(distributionEntry.getValue()[1]);
            runUpdate(UPDATE_FEED_POST_DISTRIBUTION, parameters);
        }
    }

    @Override
    public boolean updateMetaInformation(Feed feed) {
        List<Object> parameters = new ArrayList<Object>();

        // truncateToVarchar255(, "feedUrl", feed.getId()+"")

        parameters.add(truncateToVarchar255(feed.getMetaInformation().getSiteUrl(), "siteUrl", feed.getId() + ""));
        parameters.add(feed.getMetaInformation().getAddedSQLTimestamp());
        parameters.add(truncateToVarchar255(feed.getMetaInformation().getTitle(), "title", feed.getId() + ""));
        parameters.add(truncateToVarchar255(feed.getMetaInformation().getLanguage(), "language", feed.getId() + ""));
        parameters.add(feed.getMetaInformation().getByteSize());
        parameters.add(feed.getMetaInformation().getCgHeaderSize());
        parameters.add(feed.getMetaInformation().isSupportsPubSubHubBub());
        parameters.add(feed.getMetaInformation().isAccessible());
        parameters.add(feed.getMetaInformation().getFeedFormat());
        parameters.add(feed.getMetaInformation().hasItemIds());
        parameters.add(feed.getMetaInformation().hasPubDate());
        parameters.add(feed.getMetaInformation().hasCloud());
        parameters.add(feed.getMetaInformation().getRssTtl());
        parameters.add(feed.getMetaInformation().hasSkipHours());
        parameters.add(feed.getMetaInformation().hasSkipDays());
        parameters.add(feed.getMetaInformation().hasUpdated());
        parameters.add(feed.getMetaInformation().hasPublished());
        
        parameters.add(feed.getId());
        return runUpdate(UPDATE_FEED_META_INFORMATION, parameters) != -1;
    }

    /**
     * Get all polls that have been made to one feed.
     * 
     * @param feedID The feed to get information about.
     * @return A list with information about a all polls.
     */
    public List<PollMetaInformation> getFeedPollsByID(int feedID) {
        return runQuery(new FeedPollRowConverter(), GET_FEED_POLLS_BY_ID, feedID);
    }


    /**
     * Get information about a single poll, identified by feedID and pollTimestamp, from table feed_polls.
     * 
     * @param feedID The feed to get information about.
     * @param timestamp The timestamp of the poll to get information about.
     * @return Information about a single poll.
     */
    public PollMetaInformation getFeedPoll(int feedID, Timestamp timestamp) {
        return runSingleQuery(new FeedPollRowConverter(), GET_FEED_POLL_BY_ID_TIMESTAMP, feedID, timestamp);
    }

    /**
     * Get information about a single poll, identified by feedID and pollTimestamp, from table feed_polls.
     * Instead of requesting the poll at the specified timestamp, the next poll is returned whose
     * timestamp is newer or equal to {@code timestampBefore}.
     * 
     * @param feedID The feed to get information about.
     * @param timestampBefore The timestamp to get the chronologically next poll.
     * @return Information about a single poll that was subsequent to the provided timestamp.
     */
    public PollMetaInformation getNextFeedPoll(int feedID, Timestamp timestampBefore) {
        return runSingleQuery(new FeedPollRowConverter(), GET_NEXT_FEED_POLL_BY_ID_AND_TIME, feedID, timestampBefore);
    }

    /**
     * @return <code>true</code> if feed poll information have been added, <code>false</code> otherwise.
     */
    @Override
    public boolean addFeedPoll(PollMetaInformation pollMetaInfo) {

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(pollMetaInfo.getFeedID());
        parameters.add(pollMetaInfo.getPollSQLTimestamp());
        parameters.add(truncateToVarchar255(pollMetaInfo.getHttpETag(), "lastETag", pollMetaInfo.getFeedID() + ""));
        parameters.add(pollMetaInfo.getHttpDateSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpLastModifiedSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpExpiresSQLTimestamp());
        parameters.add(pollMetaInfo.getNewestItemSQLTimestamp());
        parameters.add(pollMetaInfo.getNumberNewItems());
        parameters.add(pollMetaInfo.getWindowSize());
        parameters.add(pollMetaInfo.getHttpStatusCode());
        parameters.add(pollMetaInfo.getResponseSize());

        return runInsertReturnId(ADD_FEED_POLL, parameters) != -1;
    }

    /**
     * Update a feed poll identified by id and pollTimestamp
     * 
     * @return <code>true</code> if feed poll information have been added, <code>false</code> otherwise.
     */
    public boolean updateFeedPoll(PollMetaInformation pollMetaInfo) {

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(truncateToVarchar255(pollMetaInfo.getHttpETag(), "lastETag", pollMetaInfo.getFeedID() + ""));
        parameters.add(pollMetaInfo.getHttpDateSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpLastModifiedSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpExpiresSQLTimestamp());
        parameters.add(pollMetaInfo.getNewestItemSQLTimestamp());
        parameters.add(pollMetaInfo.getNumberNewItems());
        parameters.add(pollMetaInfo.getWindowSize());
        parameters.add(pollMetaInfo.getHttpStatusCode());
        parameters.add(pollMetaInfo.getResponseSize());
        parameters.add(pollMetaInfo.getFeedID());
        parameters.add(pollMetaInfo.getPollSQLTimestamp());

        return runUpdate(UPDATE_FEED_POLL, parameters) != -1;
    }

    /**
     * Add the feed's cached items (item hash and corrected publish date) to database.
     * 
     * @param feed
     * @return true if all items have been added.
     */
    private boolean addCacheItems(Feed feed) {

        List<List<Object>> batchArgs = new ArrayList<List<Object>>();
        Map<String, Date > cachedItems = feed.getCachedItems();
        for (String hash : cachedItems.keySet()) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feed.getId());
            parameters.add(hash);
            parameters.add(cachedItems.get(hash));
            batchArgs.add(parameters);
        }

        int[] result = runBatchInsertReturnIds(ADD_CACHE_ITEMS, batchArgs);

        return (result.length == cachedItems.size());
    }

    /**
     * Get all cached items (hash, publish date) from this feed.
     * 
     * @param id The feed id.
     * @return All cached items (hash, publish date) or empty map if no item is cached. Never <code>null</code>.
     */
    private Map<String, Date> getCachedItemsById(int id) {
        Map<String, Date> cachedItems = new HashMap<String, Date>();

        List<CachedItem> itemList = runQuery(new FeedCacheItemRowConverter(), GET_CACHE_ITEMS_BY_ID, id);
        for(CachedItem item : itemList){
            cachedItems.put(item.getHash(), item.getCorrectedPublishDate());
        }

        return cachedItems;
    }

    /**
     * Deletes the feed's cached items (item hash and corrected publish date)
     * 
     * @param id the feed whose items are to delete
     * @return <code>true</code> if items were deleted, <code>false</code> in case of an error.
     */
    private boolean deleteCachedItemById(int id) {
        return runUpdate(DELETE_CACHE_ITEMS_BY_ID, id) >= 0;
    }
}