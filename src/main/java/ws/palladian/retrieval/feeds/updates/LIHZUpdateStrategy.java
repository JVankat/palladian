package ws.palladian.retrieval.feeds.updates;

import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * An implementation of the update strategy described in [LIHZ08]
 * 
 * [LIHZ08] Lee, B. S.; Im, J. W.; Hwang, B. & Zhang, D. Design of an RSS Crawler with Adaptive Revisit Manager Proc. of
 * 20th International Conference on Software Engineering and Knowledge Engineering--SEKE'08, 2008
 * 
 * @author Sandro Reichert
 * 
 */
public class LIHZUpdateStrategy extends UpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(IndHistUpdateStrategy.class);

    /**
     * Identifier to be used to store the trained model as additional date with the feed.
     */
    private static final String MODEL_IDENTIFIER = "LIHZ08Model";

    /**
     * The threshold theta.
     */
    private double thresholdTheta;

    /**
     * defined by paper authors
     */
    private static final double LIHZ_ALPHA = 0.9;

    /**
     * @param thresholdTheta The threshold theta to be used.
     */
    public LIHZUpdateStrategy(double thresholdTheta) {
        super();
        this.thresholdTheta = thresholdTheta;
    }

    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {
        if (feed.getLastPollTime() == null) {
            LOGGER.fatal("Feed id " + feed.getId()
                    + " has no lastPollTime. Cant predict next poll. Setting interval to standard.");
            feed.setUpdateInterval(getAllowedUpdateInterval(FeedReader.DEFAULT_CHECK_TIME));

        } else {

            int[][] dailyRates = getModelFromFeed(feed);
            int dayOfWeek = feed.getLastPollTime().getDay();

            // process current day, update model in case of new items
            if (feed.hasNewItem()) {
                // the algorithm is very simple. it assumes that the update happened today which might be wrong
                // but the paper does not state something more intelligent ...
                dailyRates[dayOfWeek][0]++;
                dailyRates[7][0]++;
            }
            int checkInterval = 0;

            if (trainingMode) {
                // fix training interval of 1 day
                checkInterval = 1440;
                dailyRates[dayOfWeek][1]++;
                dailyRates[7][1]++;

            } else {

                // empty feeds
                if (dailyRates[7][0] == 0.0) {
                    checkInterval = FeedReader.DEFAULT_CHECK_TIME;

                    // setting an interval other than 1 day is not provided by the feeds model so we increase the
                    // dailyRates at the first poll of the next day to make sure there cant be any item found at this
                    // day anymore.
                    // if you find this comment when debugging, behaviour for empty feeds must be changed.
                    if (feed.getLastPollTime().getHours() < checkInterval / FeedReader.DEFAULT_CHECK_TIME) {
                        int yesterday = (dayOfWeek + 6) % 7;
                        dailyRates[yesterday][1]++;
                        dailyRates[7][1]++;
                    }

                } else {

                    int simulatedDayOfWeek = dayOfWeek;
                    double cumProb = 0;

                    // done at least once, loop as many days as required to reach the threshold
                    while (cumProb < thresholdTheta
                            && (checkInterval + 1440 <= getHighestUpdateInterval() || getHighestUpdateInterval() == -1)) {
                        // increase for last iteration
                        dailyRates[simulatedDayOfWeek][1]++;
                        dailyRates[7][1]++;

                        checkInterval += 1440;
                        simulatedDayOfWeek = (simulatedDayOfWeek + 1) % 7;

                        cumProb += getProbability(dailyRates, simulatedDayOfWeek);
                    }

                }
            }
            feed.addAdditionalData(MODEL_IDENTIFIER, dailyRates);
            feed.setUpdateInterval(getAllowedUpdateInterval(checkInterval));
        }
    }

    /**
     * @param dailyRates
     * @param simulatedDayOfWeek
     * @return
     */
    private double getProbability(int[][] dailyRates, int simulatedDayOfWeek) {
        double cumProb = LIHZ_ALPHA 
                * (double) dailyRates[simulatedDayOfWeek][0] / (double) dailyRates[simulatedDayOfWeek][1] 
                + (1 - LIHZ_ALPHA) * dailyRates[7][0] / (double) dailyRates[7][1];
        return cumProb;
    }

    /**
     * Get model from the feed. In case there is no model, get it from db.
     * 
     * @param feed
     * @return The trained model for the current feed, i.e. hourly update rates for hours 0-23.
     */
    protected int[][] getModelFromFeed(Feed feed) {

        // ______ | sun | mon | tue | wed | thu | fri | sat | sun | sumOfRow
        // #found | 2___| 2
        // #weeks | 3___| 2
        // means within 3 weeks, there were two Sundays with updates. Today is Monday, since #weeks is smaller than
        // monday's value.

        int[][] dailyRates = new int[8][2];
        Map<String, Object> additionalData = feed.getAdditionalData();
        if (!additionalData.containsKey(MODEL_IDENTIFIER)) {
            if (feed.getChecks() > 0) {
                LOGGER.error("Feed id " + feed.getId() + " contains no model for strategy LIHZ08, creating a new one.");
            }
            return dailyRates;
        }
        return (int[][]) feed.getAdditionalData().get(MODEL_IDENTIFIER);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "LIHZ_" + thresholdTheta;
    }

    /**
     * @return always <code>true</code>
     */
    @Override
    public boolean hasExplicitTrainingMode() {
        // TODO Auto-generated method stub
        return true;
    }

}
