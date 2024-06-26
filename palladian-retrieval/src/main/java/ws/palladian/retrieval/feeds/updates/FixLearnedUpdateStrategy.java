package ws.palladian.retrieval.feeds.updates;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Update the check intervals in fixed learned mode.<br />
 * <br />
 * 0: Mode window (default). We use the first window and calculate the fix interval from it as<br />
 * interval = (t_newes - t_oldest)/(numEntries - 1).<br />
 * 1: Mode Poll. Additionally, use the timestamp of the first poll to calculate the interval.<br />
 * interval = (t_poll - t_oldest)/(numEntries).<br />
 * <br />
 * If (t_newes == t_oldest) or division by zero, use {@link FeedReader#DEFAULT_CHECK_TIME} as default.
 * </p>
 *
 * @author Sandro Reichert
 */
public class FixLearnedUpdateStrategy extends AbstractUpdateStrategy {

    /**
     * The update strategy has two different modes. 0: Mode window (default). We use the first window and calculate the
     * fix interval from it. 1: Mode Poll, additionally, we use the timestamp of the first poll to calculate the
     * interval.
     */
    private final int fixLearnedMode;

    private final FeedUpdateMode updateMode;

    /**
     * @param fixLearnedMode the fixLearnedMode to use; The update strategy has two different modes. 0: Mode window
     *                       (default). We use the first window and calculate the fix interval from it. 1: Mode Poll, additionally,
     *                       we use the timestamp of the first poll to calculate the interval.
     */
    public FixLearnedUpdateStrategy(int lowestInterval, int highestInterval, int fixLearnedMode, FeedUpdateMode updateMode) {
        super(lowestInterval, highestInterval);
        if (fixLearnedMode < 0 || fixLearnedMode > 1) {
            throw new IllegalArgumentException("Unsupported mode \"" + fixLearnedMode + "\". Use 0 for mode window or 1 for mode poll");
        }
        Validate.notNull(updateMode, "updateMode must not be null");
        this.fixLearnedMode = fixLearnedMode;
        this.updateMode = updateMode;
    }

    /**
     * <p>
     * Update the update interval for the feed given the post statistics.
     * </p>
     *
     * @param feed         The feed to update.
     * @param fps          This feeds feed post statistics.
     * @param trainingMode Ignored parameter. The strategy does not support an explicit training mode. The checkInterval
     *                     is automatically learned at the first poll.
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {

        int fixedCheckInterval = 0;

        // determine check interval at the very first poll
        if (feed.getChecks() == 0) {

            // set default value to be used if we cant compute an interval from feed (e.g. feed has no items)
            fixedCheckInterval = DEFAULT_CHECK_TIME;

            List<FeedItem> entries = feed.getItems();

            // use first window only
            Date intervalStartTime = feed.getOldestFeedEntryCurrentWindow();
            if (fixLearnedMode == 0) {
                Date intervalStopTime = feed.getLastFeedEntry();
                long intervalLength = DateHelper.getIntervalLength(intervalStartTime, intervalStopTime);
                if (entries.size() >= 2 && intervalLength > 0) {
                    fixedCheckInterval = (int) (intervalLength / ((entries.size() - 1) * TimeUnit.MINUTES.toMillis(1)));
                }
            }
            // use first window and first poll time
            else if (fixLearnedMode == 1) {
                Date intervalStopTime = feed.getLastPollTime();
                long intervalLength = DateHelper.getIntervalLength(intervalStartTime, intervalStopTime);
                if (entries.size() >= 1 && intervalLength > 0) {
                    fixedCheckInterval = (int) (intervalLength / (entries.size() * TimeUnit.MINUTES.toMillis(1)));
                }
            }
        }
        // any subsequent poll
        else {
            fixedCheckInterval = feed.getUpdateInterval();
        }

        // set the (new) check interval to feed
        if (updateMode == FeedUpdateMode.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedInterval(fixedCheckInterval));
        }
    }

    @Override
    public String getName() {
        if (fixLearnedMode == 0) {
            return "fixLearnedW";
        } else {
            return "fixLearnedP";
        }
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}