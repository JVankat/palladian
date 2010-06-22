package tud.iir.news;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.web.Crawler;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * FeedAggregator uses ROME library to fetch and parse feeds from the web. Feeds are stored persistently, aggregation method fetches new entries.
 * 
 * TODO add a "lastSuccessfullAggregation" attribute to feed, so we can filter out obsolute feeds. TODO we should check if an entry was modified and update.
 * 
 * https://rome.dev.java.net/ *
 * 
 * @author Philipp Katz
 * 
 */
public class FeedAggregator {

    private static final Logger logger = Logger.getLogger(FeedAggregator.class);

    private int maxThreads = 20;

    /**
     * if enabled we use PageContentExtractor to get extract text for entries directly from their corresponding web pages if neccesary
     */
    private boolean useScraping = true;

    private FeedStore store;

    /** used for all downloading purposes */
    private Crawler crawler = new Crawler();

    public FeedAggregator() {
        store = FeedDatabase.getInstance();
    }

    /** used primarily for testing to set DummyFeedStore */
    public FeedAggregator(FeedStore store) {
        this.store = store;
    }

    /**
     * Downloads a feed from the web and parses with ROME.
     * 
     * To access feeds from outside use {@link #getFeed(String)}.
     * 
     * @param feedUrl
     * @return
     * @throws FeedAggregatorException when Feed could not be retrieved, e.g. when server is down or feed cannot be parsed.
     */
    private SyndFeed getFeedWithRome(String feedUrl) throws FeedAggregatorException {
        logger.trace(">getFeedWithRome " + feedUrl);

        SyndFeed result;

        try {

            InputStream inputStream = crawler.downloadInputStream(feedUrl);

            SyndFeedInput feedInput = new SyndFeedInput();

            // this preserves the "raw" feed data and gives direct access
            // to RSS/Atom specific elements
            // see http://wiki.java.net/bin/view/Javawsxml/PreservingWireFeeds
            feedInput.setPreserveWireFeed(true);

            // build the result
            result = feedInput.build(new XmlReader(inputStream));

        } catch (IllegalArgumentException e) {
            logger.error("getFeedWithRome " + feedUrl + " " + e.toString() + " " + e.getMessage());
            throw new FeedAggregatorException(e);
        } catch (IOException e) {
            logger.error("getFeedWithRome " + feedUrl + " " + e.toString() + " " + e.getMessage());
            throw new FeedAggregatorException(e);
        } catch (FeedException e) {
            logger.error("getFeedWithRome " + feedUrl + " " + e.toString() + " " + e.getMessage());
            throw new FeedAggregatorException(e);
        }

        logger.trace("<getFeedWithRome");
        return result;
    }

    /**
     * Get feed information about a Atom/RSS feed, using ROME library.
     * 
     * @param feedUrl
     * @return
     */
    private Feed getFeed(SyndFeed syndFeed, String feedUrl) {

        logger.trace(">getFeed " + feedUrl);
        Feed result = null;

        WireFeed wireFeed = syndFeed.originalWireFeed();

        result = new Feed();
        result.setFeedUrl(feedUrl);
        result.setSiteUrl(syndFeed.getLink());
        if (syndFeed.getTitle() != null && syndFeed.getTitle().length() > 0) {
            result.setTitle(syndFeed.getTitle().trim());
        } else {
            // fallback, use feedUrl as title
            result.setTitle(feedUrl);
        }
        result.setLanguage(syndFeed.getLanguage());

        // determine feed format
        if (wireFeed instanceof com.sun.syndication.feed.rss.Channel) {
            result.setFormat(Feed.FORMAT_RSS);
        } else if (wireFeed instanceof com.sun.syndication.feed.atom.Feed) {
            result.setFormat(Feed.FORMAT_ATOM);
        }

        // determine feed type (full, partial, none)
        if (useScraping) {
            result.setTextType(determineFeedTextType(syndFeed, feedUrl));
        }

        logger.trace("<getFeed " + result);
        return result;

    }

    /**
     * Try to determine the extent of text within a feed. We distinguish between no text {@link Feed#TEXT_TYPE_NONE}, partial text
     * {@link Feed#TEXT_TYPE_PARTIAL} and full text {@link Feed#TEXT_TYPE_FULL}.
     * 
     * @param syndFeed
     * @param feedUrl
     * @return
     */
    @SuppressWarnings("unchecked")
    private int determineFeedTextType(SyndFeed syndFeed, String feedUrl) {
        logger.trace(">determineFeedTextType " + feedUrl);

        // count iterations
        int count = 0;
        // count types
        int none = 0, partial = 0, full = 0;
        // count # errors
        int errors = 0;

        // check max. 20 feed entries.
        // stop analyzing if we have more than 5 errors
        Iterator<SyndEntry> entryIterator = syndFeed.getEntries().iterator();
        while (entryIterator.hasNext() && count < 20 && errors < 5) {

            SyndEntry entry = entryIterator.next();
            String entryLink = entry.getLink();
            String entryText = getEntryText(entry);

            if (entryLink == null || entryLink.length() == 0) {
                continue;
            }
            // some feeds provide relative URLs -- convert.
            entryLink = Helper.getFullUrl(feedUrl, entry.getLink());

            // check type of linked file; ignore audio, video or pdf files ...
            String fileType = FileHelper.getFileType(entryLink);
            if (FileHelper.isAudioFile(fileType) || FileHelper.isVideoFile(fileType) || fileType.equals("pdf")) {
                logger.debug("ignoring filetype " + fileType + " from " + entryLink);
                continue;
            }

            logger.trace("checking " + entryLink);

            // entry contains no text at all
            if (entryText == null || entryText.length() == 0) {
                logger.debug("entry " + entryLink + " contains no text");
                none++;
                count++;
                continue;
            }

            // get text content from associated web page using
            // PageContentExtractor and compare with text we got from the feed
            try {
                PageContentExtractor extractor = new PageContentExtractor();

                InputStream inputStream = crawler.downloadInputStream(entryLink);
                extractor.setDocument(new InputSource(inputStream));

                Document pageContent = extractor.getResultDocument();
                String pageText = Helper.xmlToString(pageContent);
                pageText = StringHelper.removeHTMLTags(pageText, true, true, true, true);
                pageText = StringHelper.unescapeHTMLEntities(pageText);

                // first, calculate a similarity based solely on text lengths
                float lengthSim = Helper.getLengthSim(entryText, pageText);

                // only compare text similarities, if lengths of texts do not differ too much
                if (lengthSim >= 0.9) {

                    // if text from feed entry and from web page are very
                    // similar, we can assume that we have a full text feed
                    float textSim = Helper.getLevenshteinSim(entryText, pageText);
                    if (textSim >= 0.9) {
                        logger.debug("entry " + entryLink + " seems to contain full text (textSim:" + textSim + ")");
                        full++;
                        count++;
                        continue;
                    }
                }

                // feed and page were not similar enough, looks like partial text feed
                logger.debug("entry " + entryLink + " seems to contain partial text (lengthSim:" + lengthSim + ")");
                partial++;
                count++;

            } catch (MalformedURLException e) {
                logger.error("determineFeedTextType " + entryLink + " " + e.toString() + " " + e.getMessage());
                errors++;
            } catch (IOException e) {
                logger.error("determineFeedTextType " + entryLink + " " + e.toString() + " " + e.getMessage());
                errors++;
            } catch (Exception e) {
                // FIXME in some rare cases PageContentExtractor throws a NPE,
                // I dont know yet where the problem lies, so we catch it here
                // and move an as if nothing happened :)
                logger.error("determineFeedTextType " + entryLink + " " + e.toString() + " " + e.getMessage());
                errors++;
            }
        }

        // determine type of feed by using some simple heuristics ..:
        // if feed has no entries -> we cannot determine the type
        // if more than 60 % of feed's entries contain full text -> assume full text
        // if more than 80 % of feed's entries contain no text -> assume no text
        // else --> assume partial text
        int result = Feed.TEXT_TYPE_PARTIAL;
        String resultStr = "partial";
        if (count == 0) {
            result = Feed.TEXT_TYPE_UNDETERMINED;
            resultStr = "undetermined, feed has no entries";
        } else if ((float) full / count >= 0.6) {
            result = Feed.TEXT_TYPE_FULL;
            resultStr = "full";
        } else if ((float) none / count >= 0.8) {
            result = Feed.TEXT_TYPE_NONE;
            resultStr = "none";
        }

        logger.info("feed " + feedUrl + " none:" + none + " partial:" + partial + " full:" + full + " -> " + resultStr);

        logger.trace("<determineFeedTextType " + result);
        return result;
    }

    /**
     * Get entries of specified Atom/RSS feed.
     * 
     * @param feedUrl
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<FeedEntry> getEntries(SyndFeed syndFeed) {
        logger.trace(">getEntries");

        List<FeedEntry> result = new LinkedList<FeedEntry>();

        List<SyndEntry> syndEntries = syndFeed.getEntries();
        for (SyndEntry syndEntry : syndEntries) {

            FeedEntry entry = new FeedEntry();
            // remove HTML tags and unescape HTML entities from title
            String title = syndEntry.getTitle();
            if (title != null) {
                title = StringHelper.removeHTMLTags(title, true, true, true, true);
                title = StringHelper.unescapeHTMLEntities(title);
                title = title.trim();
            }

            entry.setTitle(title);

            // some feeds provide relative URLs -- convert.
            String entryLink = entry.getLink();
            if (entryLink != null && entryLink.length() > 0) {
                entryLink = Helper.getFullUrl(syndFeed.getLink(), entry.getLink());
            }
            entry.setLink(syndEntry.getLink());

            Date publishDate = syndEntry.getPublishedDate();
            if (publishDate == null) {
                // if no publish date is provided, we take the update instead
                // TODO there are still some entries without date
                publishDate = syndEntry.getUpdatedDate();
            }
            entry.setPublished(publishDate);

            String entryText = getEntryText(syndEntry);
            entry.setText(entryText);

            // Entry's assigned Tags, if any
            List<SyndCategory> categories = syndEntry.getCategories();
            for (SyndCategory category : categories) {
                entry.addTag(category.getName().replace(",", " ").trim());
            }

            // get ID information from raw feed entries
            String rawId = null;
            Object wireEntry = syndEntry.getWireEntry();
            if (wireEntry instanceof com.sun.syndication.feed.atom.Entry) {
                com.sun.syndication.feed.atom.Entry atomEntry = (com.sun.syndication.feed.atom.Entry) wireEntry;
                rawId = atomEntry.getId();
            } else if (wireEntry instanceof com.sun.syndication.feed.rss.Item) {
                com.sun.syndication.feed.rss.Item rssItem = (com.sun.syndication.feed.rss.Item) wireEntry;
                Guid guid = rssItem.getGuid();
                if (guid != null) {
                    rawId = guid.getValue();
                }
            }
            // fallback -- if we can get no ID from the feed,
            // we take the Link as identification instead
            if (rawId == null) {
                rawId = syndEntry.getLink();
                logger.trace("id is missing, taking link instead");
            }
            entry.setRawId(rawId);

            // logger.trace(entry);
            result.add(entry);
        }

        logger.trace("<getEntries");
        return result;
    }

    /**
     * Try to get the text content from SyndEntry; either from content/summary/description element. Returns null if no text content exists.
     * 
     * @param syndEntry
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getEntryText(SyndEntry syndEntry) {
        logger.trace(">getEntryText");

        // get content from SyndEntry
        // either from content or from description
        String entryText = null;
        List<SyndContent> contents = syndEntry.getContents();
        if (contents != null) {
            for (SyndContent content : contents) {
                if (content.getValue() != null && content.getValue().length() != 0) {
                    entryText = content.getValue();
                }
            }
        }
        if (entryText == null) {
            if (syndEntry.getDescription() != null) {
                entryText = syndEntry.getDescription().getValue();
            }
        }

        // clean up --> strip out HTML tags, unescape HTML code
        if (entryText != null) {
            entryText = StringHelper.removeHTMLTags(entryText, true, true, true, true);
            entryText = StringHelper.unescapeHTMLEntities(entryText);
            entryText = entryText.trim();
        }
        logger.trace("<getEntryText "/* + entryText */);
        return entryText;
    }

    /**
     * Adds a new feed for aggregation.
     * 
     * @param feedUrl
     * @return true, if feed was added.
     */
    public boolean addFeed(String feedUrl) {
    	return addFeed(feedUrl, null);
    }
    public boolean addFeed(String feedUrl, String concept) {
        logger.trace(">addFeed " + feedUrl);
        boolean added = false;

        Feed feed = store.getFeedByUrl(feedUrl);
        if (feed == null) {
            try {
                SyndFeed syndFeed = getFeedWithRome(feedUrl);
                // TODO check how old feeds is,
                // dont add feeds which were updated one year ago or more ...
                feed = getFeed(syndFeed, feedUrl);
                store.addFeed(feed);
                logger.info("added feed to store " + feedUrl);
                added = true;
            } catch (FeedAggregatorException e) {
                logger.error("error adding feed " + feedUrl + " " + e.getMessage());
            }
        } else {
            logger.info("i already have feed " + feedUrl);
        }
        
        if (feed != null && concept != null) {
        	// TODO
        	((FeedDatabase) store).assignConcept(concept, feed);
        }

        logger.trace("<addFeed " + added);
        return added;
    }

    public boolean updateFeed(Feed feed) {
        return store.updateFeed(feed);
    }

    /**
     * Add a Collection of feedUrls for aggregation. This process runs threaded. Use {@link #setMaxThreads(int)} to set the maximum number of concurrently
     * running threads.
     * 
     * @param feedUrls
     * @return number of added feeds.
     */
    public int addFeeds(Collection<String> feedUrls) {

        // Stack to store the URLs we will add
        final Stack<String> urlStack = new Stack<String>();
        urlStack.addAll(feedUrls);

        // Counter for active Threads
        final Counter threadCounter = new Counter();

        // Counter for # of added Feeds
        final Counter addCounter = new Counter();

        // stop time for adding
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // reset traffic counter
        crawler.setTotalDownloadSize(0);

        while (urlStack.size() > 0) {
            final String currentUrl = urlStack.pop();

            // if maximum # of Threads are already running, wait here
            while (threadCounter.getCount() >= maxThreads) {
                logger.trace("max # of Threads running. waiting ...");
                ThreadHelper.sleep(1000);
            }

            threadCounter.increment();
            Thread addThread = new Thread() {
                @Override
                public void run() {
                    try {
                        boolean added = addFeed(currentUrl);
                        if (added) {
                            addCounter.increment();
                        }
                    } finally {
                        threadCounter.decrement();
                    }
                }
            };
            addThread.start();

        }

        // keep on running until all Threads have finished and
        // the Stack is empty
        while (threadCounter.getCount() > 0 || urlStack.size() > 0) {
            ThreadHelper.sleep(1000);
            logger.trace("waiting ... threads:" + threadCounter.getCount() + " stack:" + urlStack.size());
        }

        stopWatch.stop();

        logger.info("-------------------------------");
        logger.info(" added " + addCounter.getCount() + " new feeds");
        logger.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        logger.info(" traffic: " + crawler.getTotalDownloadSize(Crawler.MEGA_BYTES) + " MB");
        logger.info("-------------------------------");

        return addCounter.getCount();

        // int addedCount = 0;
        // for (String feedUrl : feedUrls) {
        // boolean added = this.addFeed(feedUrl);
        // if (added) {
        // addedCount++;
        // }
        // }
        // logger.info("---------------");
        // logger.info(" added " + addedCount + " new feeds");
        // logger.info("---------------");
        // return addedCount;

    }

    /**
     * Do the aggregation process. New entries from all known feeds will be aggregated. Use {@link #setMaxThreads(int)} to set the number of maximum parallel
     * threads.
     * 
     * @return number of aggregated new entries.
     */
    public int aggregate() {
        logger.trace(">aggregate");

        List<Feed> feeds = store.getFeeds();
        logger.info("# feeds in the store " + feeds.size());

        Stack<Feed> feedsStack = new Stack<Feed>();
        feedsStack.addAll(feeds);

        // count number of running Threads
        final Counter threadCounter = new Counter();

        // count number of new entries
        final Counter newEntriesTotal = new Counter();

        // count number of encountered errors
        final Counter errors = new Counter();

        // count number of scraped pages
        final Counter scrapes = new Counter();
        final Counter scrapeErrors = new Counter();

        // stopwatch for aggregation process
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // reset traffic counter
        crawler.setTotalDownloadSize(0);

        while (feedsStack.size() > 0) {
            final Feed feed = feedsStack.pop();

            // if maximum # of Threads are already running, wait here
            while (threadCounter.getCount() >= maxThreads) {
                logger.trace("max # of Threads running. waiting ...");
                ThreadHelper.sleep(1000);
            }

            threadCounter.increment();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int newEntries = 0;
                    logger.debug("aggregating entries from " + feed.getFeedUrl());
                    try {
                        SyndFeed syndFeed = getFeedWithRome(feed.getFeedUrl());
                        List<FeedEntry> entries = getEntries(syndFeed);

                        // if PageContentExtractor fails more than 10 times, stop scraping
                        int extractorFails = 0;

                        for (FeedEntry entry : entries) {

                            // TODO stop if way have very muuuuuuch entries
                            // for example i found a feed with over 1000 entries
                            // this gets ugly when we use PageContentExtractor :(

                            // if we dont have it, add it
                            boolean add = (store.getEntryByRawId(entry.getRawId()) == null);
                            if (add) {
                                if (useScraping && extractorFails < 5 && (feed.getTextType() != Feed.TEXT_TYPE_FULL)) {
                                    logger.trace("scraping " + entry.getLink());
                                    // here we scrape content using PageContentExtractor
                                    try {
                                        PageContentExtractor extractor = new PageContentExtractor();
                                        InputStream inpStream = crawler.downloadInputStream(entry.getLink());
                                        extractor.setDocument(new InputSource(inpStream));
                                        entry.setPageText(extractor.getResultText());
                                        scrapes.increment();
                                    } catch (IOException e) {
                                        logger.trace("aggregate " + feed.getFeedUrl() + " " + e.getMessage());
                                        extractorFails++;
                                    } catch (PageContentExtractorException e) {
                                        logger.trace("aggregate " + feed.getFeedUrl() + " " + e.getMessage());
                                        extractorFails++;
                                    }
                                }
                                store.addEntry(feed, entry);
                                newEntries++;
                            }
                            // boolean added = store.addEntry(feed, entry);
                            // if (added) {
                            // logger.trace("added new entry " + entry);
                            // newEntries++;
                            // }
                        }
                        scrapeErrors.increment(extractorFails);
                    } catch (FeedAggregatorException e) {
                        errors.increment();
                    } finally {
                        threadCounter.decrement();
                    }
                    if (newEntries > 0) {
                        logger.info("# new entries in " + feed.getFeedUrl() + " " + newEntries);
                        newEntriesTotal.increment(newEntries);
                    }
                }
            };
            new Thread(runnable).start();
        }

        // keep on running until all Threads have finished and
        // the Stack is empty
        while (threadCounter.getCount() > 0 || feedsStack.size() > 0) {
            ThreadHelper.sleep(1000);
            logger.trace("waiting ... threads:" + threadCounter.getCount() + " stack:" + feedsStack.size());
        }
        stopWatch.stop();

        logger.info("-------------------------------");
        logger.info(" # of aggregated feeds: " + feeds.size());
        logger.info(" # new entries total: " + newEntriesTotal.getCount());
        logger.info(" # errors: " + errors.getCount());
        logger.info(" scraping enabled: " + useScraping);
        logger.info(" # scraped pages: " + scrapes);
        logger.info(" # scrape errors: " + scrapeErrors);
        logger.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        logger.info(" traffic: " + crawler.getTotalDownloadSize(Crawler.MEGA_BYTES) + " MB");
        logger.info("-------------------------------");

        logger.trace("<aggregate");
        return newEntriesTotal.getCount();
    }

    /**
     * Sets the maximum number of parallel threads when aggregating or adding multiple new feeds.
     * 
     * @param maxThreads
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * If enabled, we use {@link PageContentExtractor} to analyse feed type and to extract more text from feed entries with only partial text representations.
     * Keep in mind that this causes heavy traffic and takes a lot of more time than a simple aggregation process from XML feeds only.
     * 
     * @param usePageContentExtractor
     */
    public void setUseScraping(boolean usePageContentExtractor) {
        this.useScraping = usePageContentExtractor;
    }

    /**
     * Returns feed object which contains feed specific information for a specified feed URL.
     * 
     * @param feedUrl
     * @return
     * @throws FeedAggregatorException
     */
    public Feed getFeed(String feedUrl) throws FeedAggregatorException {
        SyndFeed syndFeed = getFeedWithRome(feedUrl);
        Feed feed = getFeed(syndFeed, feedUrl);
        return feed;
    }

    /**
     * Returns entries from a specified feed URL.
     * 
     * @param feedUrl
     * @return
     * @throws FeedAggregatorException
     */
    public List<FeedEntry> getEntries(String feedUrl) throws FeedAggregatorException {
        SyndFeed syndFeed = getFeedWithRome(feedUrl);
        List<FeedEntry> entries = getEntries(syndFeed);
        return entries;
    }

    // //////////////////////
    // just for testing purposes
    int getFeedTextType(String feedUrl) throws FeedAggregatorException {
        SyndFeed syndFeed = getFeedWithRome(feedUrl);
        return determineFeedTextType(syndFeed, feedUrl);
    }

    /**
     * Main method with command line interface.
     * 
     * @param args
     */
    public static void main(String[] args) {

        FeedAggregator aggregator = new FeedAggregator();

        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            String arg = args[i++];

            if (arg.equals("-threads")) {
                if (i < args.length) {
                    aggregator.setMaxThreads(Integer.valueOf(args[i++]));
                } else {
                    System.err.println("-threads requires # of threads");
                }
            }
            if (arg.equals("-noScraping")) {
                aggregator.setUseScraping(false);
            } else if (arg.equals("-add")) {
                if (i < args.length) {
                    aggregator.addFeed(args[i++]);
                } else {
                    System.err.println("-add requires a URL");
                }
            } else if (arg.equals("-addOpml")) {
                if (i < args.length) {
                    File opmlFile = new File(args[i++]);
                    List<Feed> feeds = OPMLHelper.readOPMLFile(opmlFile);
                    List<String> urls = new LinkedList<String>();
                    for (Feed feed : feeds) {
                        urls.add(feed.getFeedUrl());
                    }
                    System.out.println("adding " + urls.size() + " feeds");
                    aggregator.addFeeds(urls);
                } else {
                    System.err.println("-addOpml requires a filename");
                }
            } else if (arg.equals("-aggregate")) {
                aggregator.aggregate();
            }
            // allows continuous running of the aggregation process; we wait for
            // the specified time in minutes after each aggregation run.
            else if (arg.equals("-aggregateWait")) {
                if (i < args.length) {
                    int wait = Integer.valueOf(args[i++]);
                    while (true) {
                        aggregator.aggregate();
                        System.out.println("sleeping for " + wait + " minutes");
                        ThreadHelper.sleep(wait * DateHelper.MINUTE_MS);
                    }
                } else {
                    System.err.println("-aggregateInterval requires a numeric interval");
                }
            }
        }

        if (i == 0) {
            System.err
                    .println("CLI usage: FeedAggregator [-threads nn] [-noScraping] [-add <feed-Url>] [-addOpml <OPML-file>] [-aggregate] [-aggregateWait <minutes>]");
            System.err.println("Examples:  FeedAggregator -threads 5 -addOpml temp/cities.opml");
            System.err.println("           FeedAggregator -aggregate");
        }

    }

}
