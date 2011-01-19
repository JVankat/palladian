package tud.iir.news;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;
import tud.iir.web.HeaderInformation;
import tud.iir.web.URLDownloader;
import tud.iir.web.URLDownloader.URLDownloaderCallback;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * NewsAggregator uses ROME library to fetch and parse feeds from the web. Feeds are stored persistently, aggregation
 * method fetches new entries.
 * 
 * https://rome.dev.java.net/ *
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class FeedDownloader {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDownloader.class);

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * If enabled, we use the last poll time and the last ETag of the feed as HTTP headers when requesting the URL. This
     * way we can save bandwidth for feeds that support the HTTP 304 "Not modified" status code (about 83% of all feeds
     * do support either ETag or LastModifiedSince).
     */
    private boolean useBandwidthSavingHTTPHeaders = false;

    /** Used for all downloading purposes. */
    private Crawler crawler = new Crawler();

    /**
     * Downloads a feed from the web and parses with ROME.
     * 
     * To access feeds from outside use {@link #downloadFeed(String)}.
     * 
     * @param feedUrl
     * @return
     * @throws NewsAggregatorException when Feed could not be retrieved, e.g. when server is down or feed cannot be
     *             parsed.
     */

    /**
     * Get feed information about a Atom/RSS feed, using ROME library.
     * 
     * @param feedUrl
     * @return
     * @throws NewsAggregatorException
     */
    private Feed getFeed(Document feedDocument) throws NewsAggregatorException {

        SyndFeed syndFeed = buildRomeFeed(feedDocument);
        String feedUrl = syndFeed.getLink(); // TODO

        LOGGER.trace(">getFeed " + feedUrl);

        WireFeed wireFeed = syndFeed.originalWireFeed();

        Feed result = new Feed();
        result.setFeedUrl(feedUrl);
        if (syndFeed.getLink() != null) {
            result.setSiteUrl(syndFeed.getLink().trim());
        }
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

        // get Feed entries from rome and set to our feed
        List<FeedItem> entries = getEntries(syndFeed, feedDocument);
        result.setItems(entries);

        // get the size of the feed
        if (feedDocument != null) {
            result.setByteSize(PageAnalyzer.getRawMarkup(feedDocument).getBytes().length);
        }

        LOGGER.trace("<getFeed " + result);
        return result;

    }

    /**
     * Download the items for the given feed and set them.
     * 
     * @param feed
     * @throws NewsAggregatorException
     */
    public void updateItems(Feed feed) throws NewsAggregatorException {
        Feed downloadedFeed = getFeed(feed.getFeedUrl());
        feed.setItems(downloadedFeed.getItems());
    }

    private Date getPublishDate(FeedItem feedItem) {

        Date pubDate = null;

        Node node = feedItem.getNode();

        Node pubDateNode = XPathHelper.getChildNode(node, "*[contains(name(),'date') or contains(name(),'Date')]");

        try {
            pubDate = DateGetterHelper.findDate(pubDateNode.getTextContent()).getNormalizedDate();
        } catch (NullPointerException e) {
            LOGGER.warn("date format could not be parsed correctly: " + pubDateNode + ", feed: "
                    + feedItem.getFeedUrl() + ", " + e.getMessage());
        } catch (DOMException e) {
            LOGGER.warn("date format could not be parsed correctly: " + pubDateNode + ", feed: "
                    + feedItem.getFeedUrl() + ", " + e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("date format could not be parsed correctly: " + pubDateNode + ", feed: "
                    + feedItem.getFeedUrl() + ", " + e.getMessage());
        }

        return pubDate;
    }

    /**
     * Get entries of specified Atom/RSS feed.
     * 
     * @param feedUrl
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<FeedItem> getEntries(SyndFeed syndFeed, Document feedDocument) {
        LOGGER.trace(">getEntries");

        List<FeedItem> result = new LinkedList<FeedItem>();

        // only try a certain amount of times to extract a pub date, if none is found don't keep trying
        // TODO this is not considered at the momeent?
        // int failedDateExtractions = 0;

        List<SyndEntry> syndEntries = syndFeed.getEntries();
        for (SyndEntry syndEntry : syndEntries) {

            FeedItem entry = new FeedItem();

            // //////////// title //////////////
            // remove HTML tags and unescape HTML entities from title
            String title = syndEntry.getTitle();
            if (title != null) {
                title = HTMLHelper.removeHTMLTags(title, true, true, true, true);
                title = StringEscapeUtils.unescapeHtml(title);
                title = title.trim();
            }

            entry.setTitle(title);

            // some feeds provide relative URLs -- convert.
            String entryLink = syndEntry.getLink();
            if (entryLink != null && entryLink.length() > 0) {
                entryLink = entryLink.trim();
                entryLink = Crawler.makeFullURL(syndFeed.getLink(), entryLink);
            }
            entry.setLink(entryLink);


            String entryText = getEntryText(syndEntry);
            entry.setItemText(entryText);

            // //////////// raw id //////////////
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

            // //////////// publish date //////////////
            Date publishDate = syndEntry.getPublishedDate();
            if (publishDate == null) {

                // FIXME there are still some entries without date (David: why? does rome not get some date formats?)

                // try to find the date since Rome library failed
                publishDate = getPublishDate(entry);

                // if no publish date is provided, we take the update instead
                if (publishDate == null) {
                    publishDate = syndEntry.getUpdatedDate();
                    // failedDateExtractions++;
                } else {
                    LOGGER.debug("found publish date in original feed file: " + publishDate);
                }

            }
            entry.setPublished(publishDate);

            // fallback -- if we can get no ID from the feed,
            // we take the Link as identification instead
            if (rawId == null) {
                rawId = syndEntry.getLink();
                LOGGER.trace("id is missing, taking link instead");
            }
            if (rawId != null) {
                entry.setRawId(rawId.trim());
            } else {
                LOGGER.warn("could not get id for entry");
            }

            // logger.trace(entry);
            result.add(entry);
        }

        LOGGER.trace("<getEntries");
        return result;
    }

    /**
     * Fetch associated page content using {@link PageContentExtractor} for specified FeedEntries. Do not download
     * binary files, like PDFs, audio or video files. Downloading is done concurrently.
     * XXX
     * 
     * @param feedEntry
     */
    public void fetchPageContentForEntries(List<FeedItem> feedEntries) {
        LOGGER.debug("downloading " + feedEntries.size() + " pages");

        URLDownloader downloader = new URLDownloader();
        downloader.setMaxThreads(5);
        // PageContentExtractor extractor = new PageContentExtractor();
        final Map<String, FeedItem> entries = new HashMap<String, FeedItem>();

        for (FeedItem feedEntry : feedEntries) {
            String entryLink = feedEntry.getLink();

            if (entryLink == null) {
                continue;
            }

            // check type of linked file; ignore audio, video or pdf files ...
            String fileType = FileHelper.getFileType(entryLink);
            boolean ignore = FileHelper.isAudioFile(fileType) || FileHelper.isVideoFile(fileType)
            || fileType.equals("pdf");
            if (ignore) {
                LOGGER.debug("ignoring filetype " + fileType + " from " + entryLink);
            } else {
                downloader.add(feedEntry.getLink());
                entries.put(feedEntry.getLink(), feedEntry);
            }
        }

        downloader.start(new URLDownloaderCallback() {
            @Override
            public void finished(String url, InputStream inputStream) {
                try {
                    PageContentExtractor extractor = new PageContentExtractor();
                    extractor.setDocument(new InputSource(inputStream));
                    String pageText = extractor.getResultText();
                    entries.get(url).setPageText(pageText);
                    // feedEntry.setPageContent(page);
                } catch (PageContentExtractorException e) {
                    LOGGER.error("PageContentExtractorException " + e);
                }
            }
        });
        LOGGER.debug("finished downloading");
    }

    /**
     * Try to get the text content from SyndEntry; either from content/summary/description element. Returns null if no
     * text content exists.
     * 
     * @param syndEntry
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getEntryText(SyndEntry syndEntry) {
        LOGGER.trace(">getEntryText");

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
        if (entryText == null && syndEntry.getDescription() != null) {
            entryText = syndEntry.getDescription().getValue();

        }

        // clean up --> strip out HTML tags, unescape HTML code
        if (entryText != null) {
            entryText = HTMLHelper.htmlToString(entryText, false);
            entryText = StringEscapeUtils.unescapeHtml(entryText);
            entryText = entryText.trim();
        }
        LOGGER.trace("<getEntryText ");
        return entryText;
    }

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * 
     * @param useBandwidthSavingHTTPHeaders
     */
    public void setUseBandwidthSavingHTTPHeaders(boolean useBandwidthSavingHTTPHeaders) {
        this.useBandwidthSavingHTTPHeaders = useBandwidthSavingHTTPHeaders;
    }

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * 
     * @return
     */
    public boolean isUseBandwidthSavingHTTPHeaders() {
        return useBandwidthSavingHTTPHeaders;
    }

    // public Feed downloadFeed(String feedUrl) throws NewsAggregatorException {
    // SyndFeed syndFeed = fetchRomeFeed(feedUrl);
    // Feed feed = getFeed(syndFeed, feedUrl);
    // List<FeedItem> entries = getEntries(syndFeed);
    //
    // feed.setEntries(entries);
    // feed.setPlainXML(PageAnalyzer.getRawMarkup(plainXMLFeed));
    // return feed;
    // }
    //
    // public Feed downloadFeed(Feed feedData) throws NewsAggregatorException {
    // SyndFeed syndFeed = fetchRomeFeed(feedData);
    // Feed feed = getFeed(syndFeed, feedData.getFeedUrl());
    // List<FeedItem> entries = getEntries(syndFeed);
    // feed.setEntries(entries);
    // feed.setPlainXML(PageAnalyzer.getRawMarkup(plainXMLFeed));
    // return feed;
    // }

    public Feed getFeed(String feedUrl) throws NewsAggregatorException {
        return getFeed(feedUrl, null);
    }

    public Feed getFeed(String feedUrl, HeaderInformation headerInformation) throws NewsAggregatorException {

        LOGGER.trace(">downloadFeed " + feedUrl);
        StopWatch sw = new StopWatch();

        Document feedDocument = downloadFeed(feedUrl, headerInformation);
        Feed feed = getFeed(feedDocument);

        //        Feed result = null;

        //        try {


        // create the header information to download feed only if it has changed
        // HeaderInformation headerInformation = null;

        //            if (isUseBandwidthSavingHTTPHeaders() && feed != null) {
        //                headerInformation = new HeaderInformation(feed.getLastPollTime(), feed.getLastETag());
        //            }
        //
        //            // get the XML input via the crawler, this allows to input files with the "path/to/filename.xml" schema as
        //            // well, which we use inside Palladian.
        //            /** We keep an instance of the plain parsed XML feed to do more operations if SyndFeed fails. */
        //            Document xmlDocument = crawler.getXMLDocument(feedUrl, false, headerInformation);
        //            if (xmlDocument == null) {
        //                if (crawler.getLastResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
        //                    throw new NewsAggregatorException("could not get document from " + feedUrl);
        //                } else {
        //                    // TODO return cached document here (from disk or database)
        //                    LOGGER.debug("the feed was not modified: " + feedUrl);
        //                    return null;
        //                }
        //            }

        //        } catch (IllegalArgumentException e) {
        //            LOGGER.error("getFeedWithRome " + feedUrl + " " + e.toString() + " " + e.getMessage());
        //            throw new NewsAggregatorException(e);
        //        } catch (FeedException e) {
        //            LOGGER.error("getFeedWithRome " + feedUrl + " " + e.toString() + " " + e.getMessage());
        //            throw new NewsAggregatorException(e);
        //        }

        LOGGER.debug("downloaded feed in " + sw.getElapsedTimeString());
        LOGGER.trace("<downloadFeed " + sw.getElapsedTimeString());
        //        return result;
        return feed;

    }

    private Document downloadFeed(String feedUrl, HeaderInformation headerInformation) throws NewsAggregatorException {

        Document xmlDocument = crawler.getXMLDocument(feedUrl, false, headerInformation);
        if (xmlDocument == null) {
            throw new NewsAggregatorException("could not get document from " + feedUrl);
            // if (crawler.getLastResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
            // // TODO
            // } else {
            // // TODO return cached document here (from disk or database)
            // LOGGER.debug("the feed was not modified: " + feedUrl);
            // return null;
            // }
        }

        return xmlDocument;
    }

    private SyndFeed buildRomeFeed(Document xmlDocument) throws NewsAggregatorException {

        SyndFeedInput feedInput = new SyndFeedInput();

        // this preserves the "raw" feed data and gives direct access to RSS/Atom specific elements see
        // http://wiki.java.net/bin/view/Javawsxml/PreservingWireFeeds
        feedInput.setPreserveWireFeed(true);

        SyndFeed syndFeed;

        try {
            syndFeed = feedInput.build(xmlDocument);
        } catch (IllegalArgumentException e) {
            LOGGER.error("getRomeFeed " + xmlDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new NewsAggregatorException(e);
        } catch (FeedException e) {
            LOGGER.error("getRomeFeed " + xmlDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new NewsAggregatorException(e);
        }

        return syndFeed;

    }


    public static void main(String[] args) throws Exception {

        FeedDownloader downloader = new FeedDownloader();

        Feed feed = downloader.getFeed("http://www.tagesschau.de/xml/rss2");
        //        System.out.println(feed);
        //        System.out.println(feed.getEntries());


    }

}