package ws.palladian.retrieval.feeds.meta;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.ConnectionTimeoutPool;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * <p>The MetaInformationCreator gets information about last modified since and
 * ETag support as well as information about the header size.</p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 * @author Sandro Reichert
 *
 * @deprecated Code refactored and moved to {@link MetaInformationExtractor} which is periodically called by
 *             {@link FeedTask}.
 */
@Deprecated
public final class MetaInformationCreationTask implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    private final Feed feed;
    
    private final FeedDatabase feedDatabase;

    private static final Pattern[] VALID_FEED_PATTERNS = new Pattern[] { Pattern.compile("<rss"),
            Pattern.compile("<feed"), Pattern.compile("<rdf:RDF") };

    private String currentFeedContent;

    public MetaInformationCreationTask(Feed feed, FeedDatabase dbManager) {
        this.feed = feed;
        this.feedDatabase = dbManager;
    }

    private String getContent(URL feedURL) throws IOException {
        InputStream feedInput = null;
        String ret = "";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) feedURL.openConnection();
            connection.setReadTimeout(10000);
            ConnectionTimeoutPool timeoutPool = ConnectionTimeoutPool.getInstance();
            timeoutPool.add(connection, 2 * DateHelper.MINUTE_MS);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4");
            feedInput = connection.getInputStream();
            ret = IOUtils.toString(feedInput);
        } finally {
            IOUtils.closeQuietly(feedInput);
            if (connection != null) {
                connection.disconnect();
            }
        }
        return ret;
    }

    private boolean getFeedSupportsPubSubHubBub(final URLConnection connection) throws IOException {
        if (currentFeedContent != null && currentFeedContent.contains("rel=\"hub\"")) {
            return true;
        } else {
            return false;
        }
    }

    private String getFeedVersion(SyndFeed feed) throws IllegalArgumentException, FeedException {
        return feed.getFeedType();
    }

    /**
     * @return
     * @throws FeedException
     */
    private SyndFeed getSyndFeed() throws FeedException {
        SyndFeedInput input = new SyndFeedInput();
        input.setPreserveWireFeed(true);
        StringReader currentFeedInputReader = new StringReader(currentFeedContent);
        SyndFeed feed = input.build(currentFeedInputReader);
        currentFeedInputReader.close();
        return feed;
    }


    private boolean isAccessibleFeed(HttpURLConnection connection) throws IOException {
        if (HttpURLConnection.HTTP_NOT_FOUND == connection.getResponseCode()
                || HttpURLConnection.HTTP_FORBIDDEN == connection.getResponseCode()) {
            return false;
        }
        if (currentFeedContent != null) {
            for (Pattern pattern : VALID_FEED_PATTERNS) {
                Matcher matcher = pattern.matcher(currentFeedContent);
                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create meta information, that is, find:
     * <ul>
     * <li>Etag support</li>
     * <li>last modified since support</li>
     * <li>If conditional get is supported also the size of the reply.</li>
     * </ul>
     */
    @Override
    public void run() {

        HttpURLConnection connection = null;

        try {
            URL feedURL = new URL(feed.getFeedUrl());
            connection = (HttpURLConnection) feedURL.openConnection();
            ConnectionTimeoutPool timeoutPool = ConnectionTimeoutPool.getInstance();
            timeoutPool.add(connection, 2 * DateHelper.MINUTE_MS);
            connection.setIfModifiedSince(System.currentTimeMillis() + 60000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4");
            connection.setReadTimeout(10000);
            connection.connect();

            currentFeedContent = getContent(feedURL);
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL of feed with id: " + feed.getId() + " is malformed!", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not reed from feed with id: " + feed.getId(), e);
        } finally {
            connection.disconnect();
        }

        try {
            boolean isAccessibleFeed = isAccessibleFeed(connection);
            feed.getMetaInformation().setAccessible(isAccessibleFeed);
        } catch (IOException e) {
            LOGGER.error("Unable to check if feed at URL " + feed.getFeedUrl() + " is accessible.");
        }

        

        try {
            boolean supportsPubSubHubBub = getFeedSupportsPubSubHubBub(connection);
            feed.getMetaInformation().setSupportsPubSubHubBub(supportsPubSubHubBub);
        } catch (IOException e1) {
            LOGGER.error("Could not get Content with information about PubSubHubBub information for feed with id: "
                    + feed.getId() + ".");
        }

        String feedVersion = null;
        SyndFeed syndFeed = null;
        try {
            syndFeed = getSyndFeed();
            feedVersion = getFeedVersion(syndFeed);
            feed.getMetaInformation().setFeedFormat(feedVersion);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to determine feed version.", e);
        } catch (FeedException e) {
            LOGGER.error("Unable to determine feed version.", e);
        }

//        if (syndFeed != null) {
//            boolean hasItemIds = checkForItemIds(syndFeed);
//            metaInformation.setHasItemIds(hasItemIds);
//        }
        
        if (feedVersion != null) {
            if (feedVersion.toLowerCase().contains("rss")) {
                determineRssMetaInformation(syndFeed, feed);
            } else if (feedVersion.toLowerCase().contains("atom")) {
                determineAtomMetaInformation(syndFeed, feed);
            }
        }

        try {
            boolean success = feedDatabase.updateMetaInformation(feed);
            if (!success) {
                throw new RuntimeException("Unable to store results to Database.");
            }
        } finally {
            connection.disconnect();
        }

        feed.freeMemory();

        MetaInformationCreator.counter++;
        LOGGER.info("Processed feed: "
                + feed.getId()
                + "; percent done: "
                + MathHelper.round(100 * MetaInformationCreator.counter
                        / (double) MetaInformationCreator.collectionSize, 2) + "(" + MetaInformationCreator.counter
                + ")");
    }

    /**
     * Determine Atom specific meta information.
     * @param syndFeed
     * @param metaInformation
     */
    private void determineAtomMetaInformation(SyndFeed syndFeed, Feed feed) {
        Iterator<?> it = syndFeed.getEntries().iterator();
        if (it.hasNext()) {
            SyndEntry syndEntry = (SyndEntry) it.next();
            com.sun.syndication.feed.atom.Entry atomEntry = (com.sun.syndication.feed.atom.Entry) syndEntry.getWireEntry();
            String rawId = atomEntry.getId();

            feed.getMetaInformation().setHasItemIds(rawId != null && !rawId.isEmpty());
            feed.getMetaInformation().setHasUpdated(atomEntry.getUpdated() != null);
            feed.getMetaInformation().setHasPublished(atomEntry.getPublished() != null);
        }
    }

    /**
     * Determine RSS specific meta information.
     * @param syndFeed
     * @param metaInformation
     */
    private void determineRssMetaInformation(SyndFeed syndFeed, Feed feed) {
        Iterator<?> it = syndFeed.getEntries().iterator();
        if (it.hasNext()) {
            SyndEntry syndEntry = (SyndEntry) it.next();
            
            com.sun.syndication.feed.rss.Item rssItem = (com.sun.syndication.feed.rss.Item) syndEntry.getWireEntry();
            com.sun.syndication.feed.rss.Channel channel = (com.sun.syndication.feed.rss.Channel) syndFeed.originalWireFeed();
            Guid guid = rssItem.getGuid();

            feed.getMetaInformation().setHasItemIds(guid != null && !guid.getValue().isEmpty());
            feed.getMetaInformation().setHasPubDate(rssItem.getPubDate() != null);
            feed.getMetaInformation().setHasCloud(channel.getCloud() != null);
            feed.getMetaInformation().setTtl(channel.getTtl());
            feed.getMetaInformation().setHasSkipDays(!channel.getSkipDays().isEmpty());
            feed.getMetaInformation().setHasSkipHours(!channel.getSkipHours().isEmpty());
        }
    }

//    private boolean checkForItemIds(SyndFeed syndFeed) {
//
//        boolean result = false;
//
//        Iterator<?> it = syndFeed.getEntries().iterator();
//        if (it.hasNext()) {
//            SyndEntry syndEntry = (SyndEntry) it.next();
//            Object wireEntry = syndEntry.getWireEntry();
//
//            if (wireEntry instanceof com.sun.syndication.feed.atom.Entry) {
//                com.sun.syndication.feed.atom.Entry atomEntry = (com.sun.syndication.feed.atom.Entry) wireEntry;
//                String rawId = atomEntry.getId();
//                if (rawId != null && !rawId.isEmpty()) {
//                    result = true;
//                }
//            } else if (wireEntry instanceof com.sun.syndication.feed.rss.Item) {
//                com.sun.syndication.feed.rss.Item rssItem = (com.sun.syndication.feed.rss.Item) wireEntry;
//                Guid guid = rssItem.getGuid();
//                if (guid != null && !guid.getValue().isEmpty()) {
//                    result = true;
//                }
//            }
//        }
//        return result;
//    }

}
