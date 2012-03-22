package ws.palladian.classification.webpage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.extraction.ListDiscoverer;
import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.resources.WebLink;

public abstract class RuleBasedPageClassifier<T> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(RuleBasedPageClassifier.class);

    private String pageTitle = "";
    private String pageURL = "";
    private String pageSentences = "";
    private int highestNumberOfConsecutiveSentences = 0;
    private List<WebLink> ingoingLinks = new ArrayList<WebLink>();
    private List<WebLink> outgoingLinks = new ArrayList<WebLink>();
    private Collection<String> paginationLinks = new ArrayList<String>();
    private Collection<WebImage> images = new HashSet<WebImage>();
    private Collection<String> headlineContents = new ArrayList<String>();
    private Map<String, String> metaTags = new HashMap<String, String>();

    public RuleBasedPageClassifier() {

    }

    private void reset() {
        pageTitle = "";
        pageURL = "";
        pageSentences = "";
        ingoingLinks = new ArrayList<WebLink>();
        outgoingLinks = new ArrayList<WebLink>();
        paginationLinks = new ArrayList<String>();
        images = new HashSet<WebImage>();
        headlineContents = new ArrayList<String>();
        setMetaTags(new HashMap<String, String>());
    }

    public void extractFeatures(Document document) {
        reset();

        if (document == null) {
            return;
        }

        try {
            setPageTitle(PageAnalyzer.extractTitle(document));
        } catch (Exception e) {
            e.printStackTrace();
        }
        setPageURL(document.getDocumentURI());

        metaTags = PageAnalyzer.extractMetaInformation(document);

        String pageDomain = UrlHelper.getDomain(getPageURL());

        // get headline contents
        List<Node> headlineNodes = XPathHelper.getXhtmlNodes(document, "//H1");
        for (Node node : headlineNodes) {
            headlineContents.add(node.getTextContent());
        }
        headlineNodes = XPathHelper.getXhtmlNodes(document, "//H2");
        for (Node node : headlineNodes) {
            headlineContents.add(node.getTextContent());
        }
        headlineNodes = XPathHelper.getXhtmlNodes(document, "//H3");
        for (Node node : headlineNodes) {
            headlineContents.add(node.getTextContent());
        }
        headlineNodes = XPathHelper.getXhtmlNodes(document, "//H4");
        for (Node node : headlineNodes) {
            headlineContents.add(node.getTextContent());
        }
        headlineNodes = XPathHelper.getXhtmlNodes(document, "//H5");
        for (Node node : headlineNodes) {
            headlineContents.add(node.getTextContent());
        }
        headlineNodes = XPathHelper.getXhtmlNodes(document, "//H6");
        for (Node node : headlineNodes) {
            headlineContents.add(node.getTextContent());
        }

        // get link nodes
        List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, "//A");

        for (Node node : linkNodes) {

            String linkTitle = "";
            String linkText = node.getTextContent();
            String linkUrl = "";
            try {
                linkUrl = node.getAttributes().getNamedItem("href").getTextContent();
            } catch (Exception e) {
                LOGGER.debug("link does not have href");
                // continue;
            }

            WebLink webLink = new WebLink();
            webLink.setTitle(linkTitle);
            webLink.setText(linkText);
            webLink.setUrl(linkUrl);

            if (UrlHelper.getDomain(linkUrl).equalsIgnoreCase(pageDomain) || linkUrl.indexOf("http") != 0) {
                ingoingLinks.add(webLink);
            } else {
                outgoingLinks.add(webLink);
            }

        }

        LOGGER.debug("Ingoing Links: " + ingoingLinks.size());
        LOGGER.debug("Outgoing Links: " + outgoingLinks.size());

        // set images
        PalladianContentExtractor pse = new PalladianContentExtractor();
        pse.setDocument(document);

        try {
            setImages(pse.getImages());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOGGER.debug("Images: " + images.size());

        setPageSentences(pse.getSentencesString());

        // setHighestNumberOfConsecutiveSentences(getHighestNumberOfConsecutiveSentences(pse.getSentences(),
        // HTMLHelper.documentToHTMLString(document)));

        setHighestNumberOfConsecutiveSentences(getHighestNumberOfConsecutiveSentences(pse.getSentences(),
                HtmlHelper.documentToText(document)));

        // setHighestNumberOfConsecutiveSentences(getHighestNumberOfConsecutiveSentences(pse.getSentences(),
        // pse.getSentencesString()));

        // pagination
        ListDiscoverer ld = new ListDiscoverer();
        ld.findPaginationURLs(document);
        setPaginationLinks(ld.getPaginationURLs());
        LOGGER.debug("Pagination Links: " + paginationLinks.size());
    }


    private int getHighestNumberOfConsecutiveSentences(Collection<String> sentences, String html) {

        html = HtmlHelper.stripHtmlTags(html);

        html = StringHelper.removeControlCharacters(html);

        // html = StringHelper.removeNonAsciiCharacters(html);

        int highestNumberOfConsecutiveSentences = 0;
        int consecutiveSentences = 0;

        int nextPredictedPos = -1;
        for (String sentence : sentences) {
            // sentence = StringHelper.removeNonAsciiCharacters(sentence);
            int pos = html.indexOf(sentence);
            if (nextPredictedPos == -1 || isWithinRange(pos, nextPredictedPos, 2)) {
                consecutiveSentences++;
                nextPredictedPos = pos + sentence.length() + 1;
            } else {
                if (consecutiveSentences > highestNumberOfConsecutiveSentences) {
                    highestNumberOfConsecutiveSentences = consecutiveSentences;
                }
                consecutiveSentences = 0;
                nextPredictedPos = -1;
            }
        }
        if (consecutiveSentences > highestNumberOfConsecutiveSentences) {
            highestNumberOfConsecutiveSentences = consecutiveSentences;
        }

        return highestNumberOfConsecutiveSentences;
    }

    private static boolean isWithinRange(double value1, double value2, double range) {
        double numMin = value2 - range;
        double numMax = value2 + range;

        if (value1 <= numMax && value1 >= numMin) {
            return true;
        }

        return false;
    }

    public void extractFeatures(String url) {
        DocumentRetriever c = new DocumentRetriever();
        extractFeatures(c.getWebDocument(url));
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getPageURL() {
        return pageURL;
    }

    public void setPageURL(String pageURL) {
        this.pageURL = pageURL;
    }

    public String getPageSentences() {
        return pageSentences;
    }

    public void setPageSentences(String pageSentences) {
        this.pageSentences = pageSentences;
    }

    public List<WebLink> getIngoingLinks() {
        return ingoingLinks;
    }

    public void setIngoingLinks(List<WebLink> ingoingLinks) {
        this.ingoingLinks = ingoingLinks;
    }

    public List<WebLink> getOutgoingLinks() {
        return outgoingLinks;
    }

    public void setOutgoingLinks(List<WebLink> outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }

    public Collection<String> getPaginationLinks() {
        return paginationLinks;
    }

    public void setPaginationLinks(Collection<String> paginationLinks) {
        this.paginationLinks = paginationLinks;
    }

    public Collection<WebImage> getImages() {
        return images;
    }

    public void setImages(Collection<WebImage> images) {
        this.images = images;
    }

    public abstract T classify(String url);

    public void setHighestNumberOfConsecutiveSentences(int highestNumberOfConsecutiveSentences) {
        this.highestNumberOfConsecutiveSentences = highestNumberOfConsecutiveSentences;
    }

    public int getHighestNumberOfConsecutiveSentences() {
        return highestNumberOfConsecutiveSentences;
    }

    public Collection<String> getHeadlineContents() {
        return headlineContents;
    }

    public void setHeadlineContents(Collection<String> headlineContents) {
        this.headlineContents = headlineContents;
    }

    public void setMetaTags(Map<String, String> metaTags) {
        this.metaTags = metaTags;
    }

    public Map<String, String> getMetaTags() {
        return metaTags;
    }
}
