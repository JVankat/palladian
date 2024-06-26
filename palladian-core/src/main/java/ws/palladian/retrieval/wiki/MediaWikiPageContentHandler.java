package ws.palladian.retrieval.wiki;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import ws.palladian.helper.StopWatch;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>
 * SAX handler for processing Wikipedia XML dumps. Mapping each page to a {@link WikiPage}.
 * </p>
 *
 * @author Philipp Katz
 */
class MediaWikiPageContentHandler extends DefaultHandler {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaWikiPageContentHandler.class);

    private int pageCounter;
    private final StopWatch stopWatch;

    private final Consumer<WikiPage> callback;

    private StringBuilder buffer = new StringBuilder();
    private boolean bufferText = false;

    private boolean inRevision = false;

    private String title;
    private int pageId;
    private int namespaceId;
    private String text;

    /**
     * <p>
     * Create a new {@link MediaWikiPageContentHandler}.
     * </p>
     *
     * @param callback The callback to trigger for parsed pages, not <code>null</code>.
     */
    MediaWikiPageContentHandler(Consumer<WikiPage> callback) {
        Validate.notNull(callback, "callback must not be null");
        this.callback = callback;
        this.stopWatch = new StopWatch();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("text") || qName.equals("title") || qName.equals("ns") || (qName.equals("id") && !inRevision)) {
            bufferText = true;
        }
        if (qName.equals("revision")) {
            inRevision = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("revision")) {
            inRevision = false;
        } else if (qName.equals("id") && !inRevision) {
            pageId = Integer.parseInt(getBuffer());
        } else if (qName.equals("text")) {
            text = getBuffer();
        } else if (qName.equals("title")) {
            title = getBuffer();
        } else if (qName.equals("ns")) {
            namespaceId = Integer.parseInt(getBuffer());
        } else if (qName.equals("page")) {
            processPage();
        }
    }

    private void processPage() {
        if (++pageCounter % 1000 == 0) {
            float throughput = (float) pageCounter / TimeUnit.MILLISECONDS.toSeconds(stopWatch.getElapsedTime());
            LOGGER.debug("Processed {} pages, throughput {} pages/second.", pageCounter, Math.round(throughput));
        }
        callback.accept(new WikiPage(pageId, namespaceId, title, text));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (bufferText) {
            buffer.append(ch, start, length);
        }
    }

    private String getBuffer() {
        try {
            return buffer.toString();
        } finally {
            buffer = new StringBuilder();
            bufferText = false;
        }
    }

}
