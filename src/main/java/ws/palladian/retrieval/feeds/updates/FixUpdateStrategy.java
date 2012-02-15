package ws.palladian.retrieval.feeds.updates;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * <p>
 * Update the check intervals in fixed mode.
 * </p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 * 
 */
public class FixUpdateStrategy extends UpdateStrategy {

    /** The logger for this class. */
    private final static Logger LOGGER = Logger.getLogger(FixUpdateStrategy.class);

    /**
     * The check interval in minutes.
     */
    private final int checkInterval;

    /**
     * Create strategy and set a fixed check interval in minutes larger than zero.
     * 
     * @param checkInterval Fixed check interval in minutes. Value has to be larger than zero.
     * @throws IllegalArgumentException In case the value is smaller or equal to zero.
     */
    public FixUpdateStrategy(int checkInterval) {
        super();
        if (checkInterval <= 0) {
            throw new IllegalArgumentException("A fixed check interval smaller or equal to zero is not supported.");
        }
        this.checkInterval = checkInterval;
    }

    /**
     * <p>
     * Update the update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics.
     * @param trainingMode Ignored parameter. The strategy does not support an explicit training mode.
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {

        // default value
        int fixedMinCheckInterval = FeedReader.DEFAULT_CHECK_TIME;

        // set fix interval, independent of feed, e.g. fix60 (fix1h)
        if (getCheckInterval() > 0) {
            fixedMinCheckInterval = getCheckInterval();
        } else {
            LOGGER.error("Fix interval has not been initialized, using defaul value " + FeedReader.DEFAULT_CHECK_TIME);
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == Feed.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedUpdateInterval(fixedMinCheckInterval));
        }
    }

    @Override
    public String getName() {
            return "fix" + getCheckInterval();
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

    /**
     * @return The strategy's fixed check interval in minutes, larger than zero.
     */
    public int getCheckInterval() {
        return checkInterval;
    }

}