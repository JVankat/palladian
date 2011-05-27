package ws.palladian.retrieval.feeds.updates;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * <p>
 * Predict the next item post time by looking at the feed item post distribution and remembering it.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class PostRateUpdateStrategy extends UpdateStrategy {

    @Override
    public void update(Feed feed, FeedPostStatistics fps) {
        List<FeedItem> entries = feed.getItems();

        // learn the post distribution from the last seen entry to the newest one
        // distribution minute of the day : frequency of news in that minute
        Map<Integer, int[]> postDistribution = null;

        if (feed.getChecks() == 0) {
            postDistribution = new HashMap<Integer, int[]>();

            // since the feed has no post distribution yet, we fill all minutes with 0 posts
            for (int minute = 0; minute < 1440; minute++) {
                int[] postsChances = { 0, 0 };
                postDistribution.put(minute, postsChances);
            }

        } else {
            postDistribution = feed.getMeticulousPostDistribution();

            // in benchmark mode we keep it in memory
            if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
                FeedDatabase fd = DatabaseManagerFactory.create(FeedDatabase.class);
                postDistribution = fd.getFeedPostDistribution(feed);
            }

        }

        // update the minutes where an entry could have been posted
        int minuteCounter = 0;
        long timeLastSeenEntry = Long.MIN_VALUE;
        if (feed.getLastFeedEntry() != null) {
            timeLastSeenEntry = feed.getLastFeedEntry().getTime();
        }
        int startMinute = (int) DateHelper.getTimeOfDay(fps.getTimeOldestPost(), Calendar.MINUTE);
        for (long t = fps.getTimeOldestPost(); t < fps.getTimeNewestPost() + DateHelper.MINUTE_MS; t += DateHelper.MINUTE_MS, minuteCounter++) {
            // we have counted the chances for entries before the last seen entry already, so we skip them here
            if (t <= timeLastSeenEntry) {
                continue;
            }
            int minuteOfDay = (startMinute + minuteCounter) % 1440;
            int[] postsChances = postDistribution.get(minuteOfDay);
            postsChances[1] = postsChances[1] + 1;
            postDistribution.put(minuteOfDay, postsChances);
        }

        // update the minutes where an entry was actually posted
        for (FeedItem entry : entries) {
            // we have counted the posts for entries before the last seen entry already, so we skip them here
            if (entry.getPublished() == null || entry.getPublished().getTime() <= timeLastSeenEntry) {
                continue;
            }
            int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
            int[] postsChances = postDistribution.get(minuteOfDay);
            postsChances[0] = postsChances[0] + 1;
            postDistribution.put(minuteOfDay, postsChances);
        }

        int t1 = 0, t2 = 0;
        for (Map.Entry<Integer, int[]> a : postDistribution.entrySet()) {
            t1 += a.getValue()[0];
            t2 += a.getValue()[1];
        }
        // System.out.println(t1 + "," + t2);

        feed.setMeticulousPostDistribution(postDistribution);

        // in benchmark mode we keep it in memory, in real usage, we store the distribution in the database
        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
            FeedDatabase fd = DatabaseManagerFactory.create(FeedDatabase.class);
            fd.updateFeedPostDistribution(feed, postDistribution);
        }

        // only use calculated update intervals if one full day of distribution is available already

        startMinute = 0;

        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
            startMinute = (int) DateHelper.getTimeOfDay(System.currentTimeMillis(), Calendar.MINUTE);
        } else {
            startMinute = (int) DateHelper.getTimeOfDay(feed.getBenchmarkLookupTime(), Calendar.MINUTE);
        }

        // // estimate time to next entry and time until list is full with
        // only new but one entries

        // set to one month maximum
        int minCheckInterval = 31 * 1440;
        boolean minCheckIntervalFound = false;

        // set to six month maximum
        int maxCheckInterval = 6 * 31 * 1440;

        // add up all probabilities for the coming minutes until the
        // estimated post number is 1
        int currentMinute = startMinute;
        double estimatedPosts = 0;
        for (int c = 0; c < maxCheckInterval; c++) {

            int[] postsChances = postDistribution.get(currentMinute);
            double postProbability = 0;
            if (postsChances[1] > 0) {
                postProbability = (double) postsChances[0] / (double) postsChances[1];
            }
            estimatedPosts += postProbability;

            if (estimatedPosts >= 1 && !minCheckIntervalFound) {
                minCheckInterval = c;
                minCheckIntervalFound = true;
            }

            if (estimatedPosts >= entries.size()) {
                maxCheckInterval = c;
                break;
            }

            currentMinute = (currentMinute + 1) % 1440;
        }

        if (feed.getUpdateMode() == Feed.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedUpdateInterval(minCheckInterval));
        } else {
            feed.setUpdateInterval(getAllowedUpdateInterval(maxCheckInterval));
        }
    }

    @Override
    public String getName() {
        return "postrate";
    }

}
