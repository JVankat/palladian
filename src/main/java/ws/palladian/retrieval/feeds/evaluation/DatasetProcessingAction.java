package ws.palladian.retrieval.feeds.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.HttpHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.feeds.DefaultFeedProcessingAction;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

class DatasetProcessingAction extends DefaultFeedProcessingAction {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetProcessingAction.class);
    
    private final FeedStore feedStore;
    
    DatasetProcessingAction(FeedStore feedStore) {
        this.feedStore = feedStore;
    }
    
    @Override
    public boolean performAction(Feed feed, HttpResult httpResult) {

        boolean success = true;

        List<FeedItem> newFeedEntries = feed.getNewItems();

        // get the path of the feed's folder and csv file
        String folderPath = DatasetCreator.getFolderPath(feed.getId());
        String csvFilePath = DatasetCreator.getCSVFilePath(feed.getId(), DatasetCreator.getSafeFeedName(feed.getFeedUrl()));
        // LOGGER.debug("saving feed to: " + filePath);
        success = DatasetCreator.createDirectoriesAndCSV(feed);

        List<String> newEntriesToWrite = new ArrayList<String>();
        int newItems = newFeedEntries.size();
        long pollTimestamp = feed.getLastPollTime().getTime();

        // LOGGER.debug("Feed entries: " + newFeedEntries.size());
        for (FeedItem item : newFeedEntries) {

            String fileEntry = buildCsvLine(item);
            newEntriesToWrite.add(fileEntry);
        }

        // if all entries are new, we might have checked to late and missed some entries, we mark that by a
        // special line
        // TODO feed.getChecks()>1 may be replaced by newItems<feed.getNumberOfItemsReceived() to avoid writing a MISS
        // if a feed was empty and we now found one or more items. We have to define the MISS. If we say we write a MISS
        // every time it can happen that we missed a item, feed.getChecks()>1 is correct. If we say there cant be a MISS
        // before we see the first item, feed.getChecks()>1 has to be replaced. -- Sandro 10.08.2011
        if (newItems == feed.getWindowSize() && feed.getChecks() > 1 && newItems > 0) {
            feed.increaseMisses();
            newEntriesToWrite.add("MISS;;;;;;");
            LOGGER.fatal("MISS: " + feed.getFeedUrl() + " (id " + +feed.getId() + ")" + ", checks: "
                    + feed.getChecks() + ", misses: " + feed.getMisses());
        }

        // save the complete feed gzipped in the folder if we found at least one new item or if its the first check
        if (newItems > 0 || feed.getChecks() == 1) {

            Collections.reverse(newEntriesToWrite);

            StringBuilder newEntryBuilder = new StringBuilder();
            for (String string : newEntriesToWrite) {
                newEntryBuilder.append(string).append("\n");
            }

            boolean gzWritten = writeGZ(httpResult, folderPath, pollTimestamp, "");

            LOGGER.debug("Saving new file content: " + newEntriesToWrite.toString());
            boolean fileWritten = FileHelper.appendFile(csvFilePath, newEntryBuilder);

            if (!gzWritten || !fileWritten) {
                success = false;
            }

            // there is sometimes a weird behavior of some feeds that suddenly seem to change their window size to zero.
            // In this case, we store the received content for debugging. -- Sandro 11.07.11
            // } else if (feed.getWindowSize() == 0 && feed.hasVariableWindowSize()) {
            // success = writeGZ(httpResult, folderPath, pollTimestamp, "_debug");
        }

        boolean metadata = processPollMetadata(feed, httpResult, newItems);
        if (!metadata) {
            success = false;
        }
        
        LOGGER.debug("added " + newItems + " new posts to file " + csvFilePath + " (feed: " + feed.getId() + ")");

        return success;
    }

    private static String buildCsvLine(FeedItem item) {
        
        // build csv line for new entry
        StringBuilder fileEntry = new StringBuilder();

        // item publish or updated date
        if (item.getPublished() == null) {
            fileEntry.append(DatasetCreator.NO_TIMESTAMP).append(";");
        } else {
            fileEntry.append(item.getPublished().getTime()).append(";");
        }

        // poll timestamp
        fileEntry.append(item.getFeed().getLastPollTime().getTime()).append(";");

        // item hash
        fileEntry.append(item.getHash()).append(";");

        // item title
        if (item.getTitle() == null || item.getTitle().length() == 0) {
            fileEntry.append(DatasetCreator.NO_TITLE_REPLACEMENT).append(";");
        } else {
            fileEntry.append("\"");
            fileEntry.append(StringHelper.cleanStringToCsv(item.getTitle()));
            fileEntry.append("\";");
        }

        // item link
        if (item.getLink() == null || item.getLink().length() == 0) {
            fileEntry.append(DatasetCreator.NO_LINK_REPLACEMENT).append(";");
        } else {
            fileEntry.append("\"");
            fileEntry.append(StringHelper.cleanStringToCsv(item.getLink()));
            fileEntry.append("\";");
        }

        // window size
        fileEntry.append(item.getFeed().getWindowSize()).append(";");
        return fileEntry.toString();
    }

    /**
     * Write poll meta information to db.
     */
    @Override
    public boolean performActionOnUnmodifiedFeed(Feed feed, HttpResult httpResult) {

        return processPollMetadata(feed, httpResult, null);
    }

    /**
     * Write poll meta information to db.
     */
    @Override
    public boolean performActionOnHighHttpStatusCode(Feed feed, HttpResult httpResult) {

        return processPollMetadata(feed, httpResult, null);
    }

    /**
     * Write everything that we can't parse to a gz file.
     * All data written to gz file is taken from httpResult, the Feed is taken to determine the path and
     * filename.
     */
    @Override
    public boolean performActionOnException(Feed feed, HttpResult httpResult) {

        long pollTimestamp = feed.getLastPollTime().getTime();
        boolean folderCreated = DatasetCreator.createDirectoriesAndCSV(feed);
        boolean gzWritten = false;
        if (folderCreated) {
            String folderPath = DatasetCreator.getFolderPath(feed.getId());
            gzWritten = writeGZ(httpResult, folderPath, pollTimestamp, "_unparsable");

        }

        boolean metadata = processPollMetadata(feed, httpResult, null);

        return (gzWritten && metadata);
    }

    /**
     * Write a {@link HttpResult} to compressed file.
     * 
     * @param httpResult Result to write.
     * @param folderPath Path to write file to.
     * @param pollTimestamp The timestamp the data has been requested.
     * @param special Optional label to mark poll as unparsable, etc. Set to empty string if not required.
     * @return <code>true</code> if file has been written.
     */
    private boolean writeGZ(HttpResult httpResult, String folderPath, long pollTimestamp, String special) {
        String gzPath = folderPath + pollTimestamp + "_" + DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", pollTimestamp)
                + special + ".gz";
        boolean gzWritten = HttpRetriever.saveToFile(httpResult, gzPath, true);
        if (gzWritten) {
            LOGGER.debug("Saved " + special + " feed to: " + gzPath);
        } else {
            LOGGER.error("Could not save " + special + " feed to: " + gzPath);
        }
        return gzWritten;
    }
    
    /**
     * Put data to PollMetaInformation, write to database.
     * 
     * @param feed
     * @param httpResult
     * @param newItems
     * @return
     */
    private boolean processPollMetadata(Feed feed, HttpResult httpResult, Integer newItems) {

        PollMetaInformation pollMetaInfo = new PollMetaInformation();

        pollMetaInfo.setFeedID(feed.getId());
        pollMetaInfo.setPollTimestamp(feed.getLastPollTime());
        pollMetaInfo.setHttpETag(httpResult.getHeaderString("ETag"));
        pollMetaInfo.setHttpDate(HttpHelper.getDateFromHeader(httpResult, "Date", true));
        pollMetaInfo.setHttpLastModified(HttpHelper.getDateFromHeader(httpResult, "Last-Modified", false));
        pollMetaInfo.setHttpExpires(HttpHelper.getDateFromHeader(httpResult, "Expires", false));
        pollMetaInfo.setNewestItemTimestamp(feed.getLastFeedEntry());
        pollMetaInfo.setNumberNewItems(newItems);
        pollMetaInfo.setWindowSize(feed.getWindowSize());
        pollMetaInfo.setHttpStatusCode(httpResult.getStatusCode());

        return feedStore.addFeedPoll(pollMetaInfo);
    }
    

    public static void main(String[] args) throws Exception {
        FeedParser fr = new RomeFeedParser();
        Feed feed = fr.getFeed("http://www.d3p.co.jp/rss/mobile.rdf");
        feed.setLastPollTime(new Date());
        FeedItem item = feed.getItems().get(0);
        System.out.println(buildCsvLine(item));
    }

}
