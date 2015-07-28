package ws.palladian.extraction.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.WebPageDateEvaluator;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.PageAnalyzer;
import ws.palladian.retrieval.XPathSet;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * <p>
 * The PalladianContentExtractor extracts clean sentences from (English) texts. That is, short phrases are not included
 * in the output. Consider the {@link ReadabilityContentExtractor} for general content. The main difference is that this
 * class also finds sentences in comment sections of web pages.
 * </p>
 * <p/>
 * <p>
 * Score on boilerplate dataset: 0.76088387 (r1505);
 * </p>
 *
 * @author David Urbansky
 */
public class PalladianContentExtractor extends WebPageContentExtractor {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianContentExtractor.class);

    private static final List<String> MAIN_NODE_HINTS = new ArrayList<String>();

    /**
     * The entire document.
     */
    private Document document;

    /**
     * The detected main content node.
     */
    private Node resultNode;

    /**
     * The main content node but less strict, it might contain some clutter but also more images (used to find the main
     * image).
     */
    private Node outerResultNode;

    /**
     * All sentences in the main content.
     */
    private List<String> sentences = new ArrayList<>();

    /**
     * Detected comments on the page.
     */
    private List<String> comments = new ArrayList<>();

    /**
     * The html text of the main content node.
     */
    private String mainContentHtml = "";

    /**
     * The readable text of the main content node.
     */
    private String mainContentText = "";

    /**
     * The cleansed entire text content of the page.
     */
    private String fullTextContent = "";

    /**
     * <p>
     * Extracted images will have a width and height. If the webmaster decides to specify these values in percentages we
     * take the following value as a guess of the container size in which the image is located in. Finding the real
     * width and height of the container would require too much effort and possibly CSS parsing.
     * </p>
     */
    private static final int DEFAULT_IMAGE_CONTAINER_SIZE = 500;

    private List<WebImage> imageUrls;

    static {
        MAIN_NODE_HINTS.add("articleText");
        MAIN_NODE_HINTS.add("article_body");
        MAIN_NODE_HINTS.add("article-body");
        MAIN_NODE_HINTS.add("articleBody");
        // TODO next hint "hfeed" not tested properly with evaluation dataset!
        MAIN_NODE_HINTS.add("hfeed");
        // TODO more fine tuning possible here:
        // MAIN_NODE_HINTS.add("story_body");
        // MAIN_NODE_HINTS.add("single_post_content");
        // MAIN_NODE_HINTS.add("entry-single");
        // MAIN_NODE_HINTS.add("entry-content");
        // MAIN_NODE_HINTS.add("section-entry");
        // MAIN_NODE_HINTS.add("storyText");
        // MAIN_NODE_HINTS.add("post-content");
        // MAIN_NODE_HINTS.add("post-body");
        // MAIN_NODE_HINTS.add("articleContent");
        // MAIN_NODE_HINTS.add("articleBody");
        // MAIN_NODE_HINTS.add("article-content");
        // MAIN_NODE_HINTS.add("main-content");
        // MAIN_NODE_HINTS.add("contentBody");
        // MAIN_NODE_HINTS.add("article");
        // MAIN_NODE_HINTS.add("content");
        // MAIN_NODE_HINTS.add("post");
        MAIN_NODE_HINTS.add("st_text_c");
    }

    @Override
    public PalladianContentExtractor setDocument(Document document) throws PageContentExtractorException {
        this.document = document;
        imageUrls = null;

        resultNode = null;
        outerResultNode = null;
        sentences = new ArrayList<>();
        comments = new ArrayList<>();
        mainContentHtml = "";
        mainContentText = "";
        fullTextContent = "";

        parseDocument();
        return this;
    }

    public Document getDocument() {
        return document;
    }

    public List<String> getSentences() {
        return sentences;
    }

    public List<String> getComments() {
        return comments;
    }

    private String cleanXPath(String xPath) {
        // System.out.println("before clean xpath: " + xPath);
        xPath = xPath.replaceAll("/text(\\[.*?\\])?", "/");
        xPath = xPath.replace("html/body", "");
        xPath = xPath.replace("xhtml:html/xhtml:body", "");
        // xPath = xPath.replaceAll("/font(\\[.*?\\])?", "/");
        // xPath = xPath.replaceAll("/xhtml:font(\\[.*?\\])?", "/");

        xPath = xPath.replace("///", "//");

        // in case we did not find anything, we take the body content
        if (xPath.isEmpty() || xPath.equals("//")) {
            xPath = "//body";
        }

        if (xPath.endsWith("//")) {
            xPath = xPath.substring(0, xPath.length() - 2);
        }

        // System.out.println("clean xpath: " + xPath);

        // xPath = XPathHelper.addXhtmlNsToXPath(xPath);

        return xPath;
    }

    /**
     * <p>
     * This does not only contain the main content but also comments etc.
     * </p>
     *
     * @return
     */
    public String getEntireTextContent() {
        fullTextContent = fullTextContent.replaceAll("(\t)+", "");
        fullTextContent = Pattern.compile("^.{0,40}$", Pattern.MULTILINE).matcher(fullTextContent).replaceAll("\n");
        fullTextContent = fullTextContent.replaceAll("\n(\\s)+\n", "\n\n");
        fullTextContent = fullTextContent.replaceAll("(\n){2,}", "\n\n");

        return fullTextContent;
    }

    private void parseDocument() throws PageContentExtractorException {

        String content;

        // if true, we didn't find valid elements within the main content block and take the whole node text
        boolean useMainNodeText = false;

        String parentXpath = "";
        String resultNodeXPath = "";
        resultNode = getMainContentNodeWithHints();
        int textNodeCount = 0;

        if (resultNode != null) {
            resultNodeXPath = PageAnalyzer.constructXPath(resultNode);
            resultNodeXPath = XPathHelper.addXhtmlNsToXPath(getDocument(), resultNodeXPath);
            parentXpath = resultNodeXPath;

            textNodeCount = countDirectTextNodes();
            LOGGER.debug("direct text nodes: " + textNodeCount);

            outerResultNode = resultNode;
        }

        fullTextContent = HtmlHelper.documentToText(document);
        cleanDom();
        content = HtmlHelper.documentToText(document);
        sentences = Tokenizer.getSentences(content, true);

        XPathSet xpathset = new XPathSet();

        // build xpaths to the sentences in the text, the more sentences we find in one area, the more likely it is the
        // main content
        Set<String> uniqueSentences = new HashSet<>(sentences);
        for (String sentence : uniqueSentences) {
            Set<String> xPaths = PageAnalyzer.constructAllXPaths(getDocument(), sentence);
            for (String xPath : xPaths) {
                xPath = PageAnalyzer.removeXPathIndicesFromLastCountNode(xPath);
                // XXX? not really since it is better without this if (!xPath.contains("/xhtml:li") &&
                // !xPath.contains("/li")) {
                xpathset.add(xPath);
                // }
            }
        }

        Map<String, Integer> xpmap = xpathset.getXPathMap();
        String highestCountXPath = xpathset.getHighestCountXPath();
        int highestCount = xpathset.getCountOfXPath(highestCountXPath);

        // if we know the main content block, remove all xPath which are not in that block
        Set<String> outOfMainContent = new HashSet<>();
        if (!resultNodeXPath.isEmpty()) {
            for (Entry<String, Integer> mapEntry : xpmap.entrySet()) {
                if (!mapEntry.getKey().startsWith(resultNodeXPath)) {
                    outOfMainContent.add(mapEntry.getKey());
                }
            }
            for (String string : outOfMainContent) {
                xpathset.remove(string);
            }

            if (!xpathset.isEmpty()) {
                highestCountXPath = xpathset.getHighestCountXPath();
                highestCount = xpathset.getCountOfXPath(highestCountXPath);

                if (textNodeCount > 3) {
                    useMainNodeText = true;
                }

            } else {
                useMainNodeText = true;
            }
        }

        String shortestMatchingXPath = highestCountXPath;
        if (!useMainNodeText) {
            // shorter paths with the same counts should be favored to not miss any content
            for (Entry<String, Integer> mapEntry : xpmap.entrySet()) {
                if (mapEntry.getKey().length() < shortestMatchingXPath.length() && mapEntry.getValue() == highestCount) {
                    shortestMatchingXPath = mapEntry.getKey();
                }
            }
        } else {
            parentXpath = resultNodeXPath;
        }

        if (shortestMatchingXPath.isEmpty()) {
            useMainNodeText = true;
        }

        shortestMatchingXPath = PageAnalyzer.findLastBoxSection(shortestMatchingXPath);

        // in case we did not find anything, we take the body content
        if (!useMainNodeText) {
            // parentXpath = PageAnalyzer.findLastBoxSection(shortestMatchingXPath);
            parentXpath = XPathHelper.getParentXPath(shortestMatchingXPath);
        }

        parentXpath = cleanXPath(parentXpath);

        resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);
        if (resultNode == null) {
            parentXpath = parentXpath.replaceAll("/[^x].*?:.*?/", "//");
            resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);

            if (resultNode == null) {
                parentXpath = XPathHelper.addXhtmlNsToXPath(parentXpath);
                resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);

                if (resultNode == null) {
                    // XXX
                    mainContentText = fullTextContent;
                    return;
                }
            }
        }

        if (!useMainNodeText) {

            // shortestMatchingXPath = cleanXPath(shortestMatchingXPath);

            // add possible headlines that are on the same level as the content nodes to the target text nodes
            shortestMatchingXPath = addHeadlineSiblings(shortestMatchingXPath);

            // get the clean text only
            StringBuilder cleanText = new StringBuilder();
            List<Node> contentNodes = XPathHelper.getXhtmlNodes(getDocument(), shortestMatchingXPath);

            // if (contentNodes.isEmpty()) {
            // shortestMatchingXPath = XPathHelper.addXhtmlNsToXPath(shortestMatchingXPath);
            // if (!shortestMatchingXPath.contains("::xhtml:")) {
            // shortestMatchingXPath = shortestMatchingXPath.replace("::", "::xhtml:");
            // }
            // contentNodes = XPathHelper.getXhtmlNodes(getDocument(), shortestMatchingXPath);
            // }

            for (Node node : contentNodes) {
                String textContent = node.getTextContent();
                if (!textContent.isEmpty()) {
                    cleanText.append(textContent).append("\n\n");
                }
            }

            mainContentText = cleanText.toString();
        }

        mainContentHtml = HtmlHelper.xmlToString(resultNode, true);

        // if we didn't get clean text, let's take the content of the main node
        if (mainContentText.trim().length() < 100) {
            mainContentText = HtmlHelper.documentToReadableText(resultNode);
        }
        if (mainContentText.trim().length() < 100) {
            mainContentText = fullTextContent;
        }
    }

    private int countDirectTextNodes() {
        int textNodeCount = 0;

        List<Node> breakNodes = XPathHelper.getXhtmlNodes(resultNode, "./text()");
        for (Node node : breakNodes) {
            String tc = node.getTextContent().trim();
            if (tc.length() > 20 && !tc.startsWith("<!--")) {
                textNodeCount++;
            }
        }

        return textNodeCount;
    }

    /**
     * <p>
     * Remove comment nodes, scripts, and iframes etc.
     * </p>
     */
    private void cleanDom() {

        // remove comments
        removeCommentNodes();

        // remove scripts / style / iframes etc.
        List<Node> divs = XPathHelper.getXhtmlNodes(document,
                "//*[(self::xhtml:style) or (self::xhtml:script) or (self::xhtml:iframe)]");
        for (Node node : divs) {
            if (node == null) {
                continue;
            }
            Node parentNode = node.getParentNode();
            if (parentNode == null) {
                continue;
            }
            parentNode.removeChild(node);
        }

    }

    private void removeCommentNodes() {

        List<Node> divs = XPathHelper
                .getXhtmlNodes(
                        document,
                        "//*[(self::xhtml:div) or (self::xhtml:p) or (self::xhtml:section) or (self::xhtml:ol) or (self::xhtml:ul) or (self::xhtml:li)][@class='comment' or contains(@class,'comment ') or contains(@class,' comment') or contains(@class,'comments ') or contains(@class,' comments') or contains(@id,'comments') or @id='disqus_thread']");

        for (Node node : divs) {
            comments.add(HtmlHelper.documentToReadableText(node));
            node.getParentNode().removeChild(node);
        }

    }

    private Node getMainContentNodeWithHints() {

        Node mainNode = null;

        for (String hint : MAIN_NODE_HINTS) {
            List<Node> mainNodes = XPathHelper.getXhtmlNodes(getDocument(),
                    "//*[(self::xhtml:div) or (self::xhtml:p) or (self::xhtml:span)][@class='" + hint
                            + "' or contains(@class,'" + hint + " ') or contains(@class,' " + hint
                            + "') or @itemprop='" + hint + "' or @id='" + hint + "']");

            if (!mainNodes.isEmpty()) {
                mainNode = mainNodes.get(0);
                if (mainNodes.size() > 1) {
                    mainNode = mainNode.getParentNode();
                }
            }

            if (mainNode != null) {
                LOGGER.debug("found main node with hint: " + hint);
                // System.out.println(HtmlHelper.getInnerXml(mainNode));
                break;
            }
        }

        return mainNode;
    }

    /**
     * <p>
     * Several elements are allowed to be siblings to the main text nodes (such as lists etc.)
     * </p>
     *
     * @param xPath The xPath that points to the main content nodes.
     * @return An xpath that also targets the siblings of the main text nodes.
     */
    private String addHeadlineSiblings(String xPath) {
        try {
            String[] parts = xPath.split("/");
            String lastPart = parts[parts.length - 1];
            String xhtmlNs = "";
            if (lastPart.contains("xhtml")) {
                xhtmlNs = "xhtml:";
            }
            String newLastPart = "*[(self::" + lastPart + ") or (self::" + xhtmlNs + "h1) or (self::" + xhtmlNs
                    + "h2) or (self::" + xhtmlNs + "h3) or (self::" + xhtmlNs + "h4) or (self::" + xhtmlNs
                    + "h5) or (self::" + xhtmlNs + "h6) or (self::" + xhtmlNs + "span) or (self::" + xhtmlNs
                    + "ul) or (self::" + xhtmlNs + "ol) or (self::" + xhtmlNs + "blockquote)]";
            xPath = xPath.replaceAll(lastPart + "$", newLastPart);
        } catch (Exception e) {
        }

        return xPath;
    }

    public List<WebImage> getImages(String fileType) {

        List<WebImage> filteredImages = new ArrayList<>();
        String ftSmall = fileType.toLowerCase();
        for (WebImage webImage : getImages()) {
            if (webImage.getFileType().toLowerCase().equalsIgnoreCase(ftSmall)) {
                filteredImages.add(webImage);
            }
        }

        return filteredImages;
    }

    public void filter(List<WebImage> images, String... imageFormats) {
        List<WebImage> filteredImages = new ArrayList<>();

        for (WebImage webImage : getImages()) {
            for (String imageFormat : imageFormats) {
                if (webImage.getFileType().equalsIgnoreCase(imageFormat)) {
                    filteredImages.add(webImage);
                }
            }
        }

        images.clear();
        images.addAll(filteredImages);
    }

    public List<WebImage> getImages() {
        if (outerResultNode != null) {
            return getImages(outerResultNode,getDocument());
        }
        return getImages(resultNode,getDocument());
    }

    public List<WebImage> getImages(Node imageParentNode) {
        return getImages(imageParentNode, document);
    }
    public List<WebImage> getImages(Node imageParentNode, Document webDocument) {

        if (imageUrls != null) {
            return imageUrls;
        }

        imageUrls = new ArrayList<>();

        if (imageParentNode == null) {
            return imageUrls;
        }

        // we need to query the result document with an xpath but the name space check has to be done on the original
        // document
        String imgXPath = ".//xhtml:img";

        List<Node> imageNodes = new ArrayList<>();

        while (imageNodes.isEmpty() && imageParentNode != null) {
            imageNodes = XPathHelper.getXhtmlNodes(imageParentNode, imgXPath);
            imageParentNode = imageParentNode.getParentNode();
        }

        for (Node node : imageNodes) {
            try {

                NamedNodeMap nnm = node.getAttributes();
                BasicWebImage.Builder builder = new BasicWebImage.Builder();
                String imageUrl = nnm.getNamedItem("src").getTextContent();

                if (!imageUrl.startsWith("http")) {
                    imageUrl = UrlHelper.makeFullUrl(webDocument.getDocumentURI(), null, imageUrl);
                }
                builder.setImageUrl(imageUrl);
                builder.setFileType(FileHelper.getFileType(imageUrl));

                if (nnm.getNamedItem("alt") != null) {
                    builder.setSummary(nnm.getNamedItem("alt").getTextContent());
                }
                if (nnm.getNamedItem("title") != null) {
                    builder.setTitle(nnm.getNamedItem("title").getTextContent());
                }
                if (nnm.getNamedItem("width") != null) {
                    String w = nnm.getNamedItem("width").getTextContent();
                    builder.setWidth(getImageSize(w));
                }
                if (nnm.getNamedItem("height") != null) {
                    String h = nnm.getNamedItem("height").getTextContent();
                    builder.setHeight(getImageSize(h));
                }

                imageUrls.add(builder.create());

            } catch (NumberFormatException e) {
                LOGGER.debug(e.getMessage());
            } catch (NullPointerException e) {
                LOGGER.debug("an image has not all necessary attributes");
            }
        }

        return imageUrls;
    }

    private int getImageSize(String attributeText) throws NumberFormatException {

        int size = -1;
        attributeText = attributeText.replace(",*", "");

        if (attributeText.contains("%")) {
            attributeText = attributeText.replace("%", "");
            attributeText = StringHelper.trim(attributeText);
            size = (int) (0.01 * Integer.parseInt(attributeText) * DEFAULT_IMAGE_CONTAINER_SIZE);
        } else {
            attributeText = attributeText.replace("px", "");
            attributeText = StringHelper.trim(attributeText);
            size = Integer.parseInt(attributeText);
        }

        return size;
    }

    @Override
    public Node getResultNode() {
        return resultNode;
    }

    public String getMainContentHtml() {
        return mainContentHtml;
    }

    @Override
    public String getResultText() {
        return mainContentText;
    }

    public String getSentencesString() {
        StringBuilder text = new StringBuilder();
        List<String> sentences = getSentences();

        for (String string : sentences) {
            text.append(string).append(" ");
        }

        return text.toString();
    }

    @Override
    public String getResultTitle() {
        // try to get it from the biggest headline
        Node h1Node = XPathHelper.getXhtmlNode(getDocument(), "//h1");

        String resultTitle = "";
        if (h1Node != null) {
            resultTitle = StringHelper.clean(h1Node.getTextContent());
        }
        if (resultTitle.isEmpty()) {
            Node titleNode = XPathHelper.getXhtmlNode(getDocument(), "//title");

            if (titleNode != null) {
                resultTitle = titleNode.getTextContent();

                // remove everything after | sign
                resultTitle = resultTitle.replaceAll("\\|.*", "").trim();
            } else {
                resultTitle = StringHelper.getFirstWords(mainContentText, 20);
            }
        }

        return resultTitle;
    }

    @Override
    public String getExtractorName() {
        return "Palladian";
    }

    /**
     * <p>
     * Try to find the correct image dimensions of all extracted images. Do that only for images that had no "width" and
     * "height" attributes in the image tag. Note that other images might have different real dimensions and might have
     * been scaled using the HTML attributes.
     * </p>
     */
    public void analyzeImages() {

        List<WebImage> temp = new ArrayList<>();

        for (WebImage webImage : getImages()) {
            if (webImage.getWidth() == 0 || webImage.getHeight() == 0) {
                BufferedImage image = ImageHandler.load(webImage.getUrl());
                if (image != null) {
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setWebImage(webImage);
                    builder.setWidth(image.getWidth());
                    builder.setHeight(image.getHeight());
                    temp.add(builder.create());
                } else {
                    temp.add(webImage);
                }
            } else {
                temp.add(webImage);
            }
        }

        imageUrls = temp;

    }

    /**
     * <p>
     * Get the author of the article using the WebKnox API.
     * </p>
     *
     * @param apiKey The WebKnox API key.
     * @return The detected author name.
     */
    public String getAuthorName(String apiKey) {
        String author = "";
        String url = "http://webknox.com/api/webpage/author?url=" + getDocument().getDocumentURI()
                + "&language=en&apiKey=" + apiKey;
        DocumentRetriever retriever = new DocumentRetriever();
        // changed to palladian JSON, but untested. Philipp, 2013-09-22
        String authorsJson = retriever.getText(url);
        if (authorsJson != null && authorsJson.length() > 0) {
            try {
                return new JsonArray(authorsJson).getJsonObject(0).getString("name");
            } catch (JsonException e) {
            }
        }
        return author;
    }

    /**
     * <p>
     * Get the publish date of the Web page.
     * </p>
     *
     * @return The extracted date.
     */
    public ExtractedDate getPublishDate() {
        return WebPageDateEvaluator.getBestDate(document, PageDateType.PUBLISH);
    }

    /**
     * <p>Use several indicators in the site's HTML to detect its language.</p>
     * @return
     */
    public Language detectLanguage() {

        // use TLDs first
        String uri = getDocument().getDocumentURI();

        String domain = UrlHelper.getDomain(uri);
        if (domain.endsWith(".de")) {
            return Language.GERMAN;
        } else if (domain.endsWith(".fr")) {
            return Language.FRENCH;
        } else if (domain.endsWith(".es")) {
            return Language.SPANISH;
        } else if (domain.endsWith(".it")) {
            return Language.ITALIAN;
        } else if (domain.endsWith(".co.uk") || domain.endsWith(".ie")) {
            return Language.ENGLISH;
        } else if (domain.endsWith(".pt")) {
            return Language.PORTUGUESE;
        }

        // look in HTML lang attribute <html lang="de">
        String innerXml = HtmlHelper.getInnerXml(getDocument());
        innerXml = innerXml.toLowerCase();
        String substringBetween = StringHelper.getSubstringBetween(innerXml, " lang=\"", "\"");
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, " xml:lang=\"", "\"");
        }
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, " xmlU00003Alang=\"", "\"");
        }
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, "<meta name=\"content-language\" content=\"", "\"");
        }
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, "<meta name=\"language\" content=\"", "\"");
        }
        if (substringBetween != null && !substringBetween.isEmpty() && substringBetween.length() < 6) {
            // remove country, e.g. en-US
            String[] parts = substringBetween.split("[-:]");
            return Language.getByIso6391(parts[0]);
        }

        return null;
    }

    /**
     * @param args
     * @throws PageContentExtractorException
     */
    public static void main(String[] args) throws PageContentExtractorException {

        PalladianContentExtractor palladianContentExtractor = new PalladianContentExtractor();
        palladianContentExtractor.setDocument(new DocumentRetriever().getWebDocument("http://www.funny.pt"));
        Language language = palladianContentExtractor.detectLanguage();


        System.out.println(language);
        System.exit(0);

        // ////////////////////////////////////
        // Document webDocument = new DocumentRetriever().getWebDocument("C:\\Workspace\\data\\GoldStandard\\98.html");
        // String xPath =
        // "//table[4]/tbody/tr[1]/td[1]/table[1]/tbody/tr[1]/td/table[1]/tbody/tr[1]/td[1]/table[1]/tbody/tr[2]/td[1]";
        // // xPath = "//text//table[1]/tbody/tr[2]/td[1]";
        // xPath = XPathHelper.addXhtmlNsToXPath(xPath);
        // List<Node> xhtmlNodes = XPathHelper.getXhtmlNodes(webDocument, xPath);
        // CollectionHelper.print(xhtmlNodes);
        // System.exit(0);
        // ////////////////////////////////////

        PalladianContentExtractor pe = new PalladianContentExtractor();
        // pe.setDocument("http://jezebel.com/5733078/can-you-wear-six-items-or-less");
        // pe.setDocument("http://www.seobook.com/shopping-search");
        // pe.setDocument("http://www.fourhourworkweek.com/blog/2012/11/24/the-4-hour-chef-launch-summary-of-week-one/");

        // pe.setDocument("http://www.dailyfinance.com/2012/07/20/stockbroker-corrupt-wall-street-cheats/");
        // pe.setDocument("http://www.nationalmemo.com/white-house-tax-rates-on-the-rich-will-go-up/");
        // pe.setDocument("http://news.discovery.com/human/women-prefer-thin-men-121128.html");
        // pe.setDocument("http://www.extremetech.com/extreme/141643-mits-sun-funnel-could-slit-solar-powers-efficiency-bottleneck-2");
        // pe.setDocument("http://www.cbsnews.com/8301-201_162-57556049/3-bp-employees-plead-not-guilty-on-gulf-oil-spill-charges/");
        // pe.setDocument("http://www.jta.org/news/article/2012/11/28/3113081/petitions-do-not-find-wonder-lovely");
        // pe.setDocument("http://www.walesonline.co.uk/news/wales-news/2012/11/29/leveson-report-fears-its-recommendations-could-throw-governnment-into-turmoil-91466-32329849/");
        // pe.setDocument("http://www.sfgate.com/business/energy/article/Workers-raise-1st-section-of-new-Chernobyl-shelter-4069524.php");
        // pe.setDocument("http://news.yahoo.com/germany-not-back-palestinian-u-n-bid-government-130743009.html");
        // pe.setDocument("http://www.thelocal.se/44726/20121129/");
        // pe.setDocument("http://www.politicususa.com/walmart-earns-record-profits-supporting-republicans-plan-slash-employees-food-stamps.html");

        // to solve:
        // ol/li
        // pe.setDocument("http://www.africanews.com/site/Rebels_begin_withdrawal_in_eastern_DR_Congo/list_messages/42682");
        // pe.setDocument("http://www.dailyfinance.com/2012/07/20/stockbroker-corrupt-wall-street-cheats/");
        // pe.setDocument("http://jezebel.com/5733078/can-you-wear-six-items-or-less");
        // pe.setDocument("http://slotmachinebasics.com/");
        // -> formatting
        // pe.setDocument("http://www.absoluteastronomy.com/topics/Jet_Li");
        // pe.setDocument("http://www.reuters.com/article/2012/11/15/us-usa-obama-petraeus-idUSBRE8AD1FB20121115");
        // pe.setDocument("http://gulfnews.com/opinions/editorials/israel-has-to-change-its-policy-of-violence-1.1108785");

        // pe.setDocument("http://www.cinefreaks.com/news/692/Neun-interessante-Fakten%2C-die-du-nicht-%C3%BCber-die-Oscars-2012-wusstest");
        // pe.setDocument("http://www.komonews.com/news/local/Will-feds-let-new-marijuana-law-stand-without-a-fight-177666311.html");
        // pe.setDocument("http://www.raptitude.com/2012/11/what-love-is-not/");
        // pe.setDocument("http://blogs.windows.com/windows_phone/b/windowsphone/archive/2012/11/28/an-update-on-windows-phone-7-8.aspx");
        // pe.setDocument("http://blog.mashape.com/170078722");
        // pe.setDocument("http://www.killerstartups.com/startups-tools-and-guides/launch-your-own-crazy-idea-jason-sadler/");
        // pe.setDocument("http://uncommonbusiness.blogspot.de/2012/11/savingscom-story.html");
        // pe.setDocument("http://www.daemonology.net/blog/2012-11-28-broken-EC2-firewall.html");
        // pe.setDocument("http://www.bizjournals.com/washington/blog/techflash/2012/11/major-layoffs-set-for-livingsocial.html");
        // pe.setDocument("http://blog.stephenwolfram.com/2012/11/mathematica-9-is-released-today/");
        // pe.setDocument("http://www.allaboutbirds.org/guide/Peregrine_Falcon/lifehistory");
        // pe.setDocument("http://www.hollyscoop.com/cameron-diaz/52.aspx");
        // pe.setDocument("http://www.absoluteastronomy.com/topics/Jet_Li");
        // pe.setDocument("http://www.cinefreaks.com/news/696/Die-Hard-5");
        // pe.setDocument("http://edition.cnn.com/2012/11/23/world/meast/egypt-protests/index.html?hpt=hp_t1");
        // pe.setDocument("http://www.bbc.co.uk/news/world-middle-east-20458148");
        // pe.setDocument("http://lifehacker.com/5862004/heres-your-black-friday-survival-toolkit");
        // pe.setDocument("http://www.reuters.com/article/2012/11/23/us-egypt-president-idUSBRE8AM0DO20121123");
        // pe.setDocument("http://www.foxnews.com/us/2012/11/23/walmart-calls-black-friday-success-despite-protests-about-worker-conditions/");
        // pe.setDocument("http://www.seobythesea.com/2012/11/not-all-anchor-text-is-equal-other-co-citation-observations/");
        // pe.setDocument("http://arstechnica.com/tech-policy/2012/11/ca-measure-would-ban-anonymous-online-speech-for-sex-offenders/");
        // pe.setDocument("http://www.usatoday.com/story/opinion/2012/10/31/mitt-romney-jeep-chrysler-uaw/1672501/");
        // pe.setDocument("http://www.washingtonpost.com/politics/decision2012/after-grueling-campaign-polls-open-for-election-day-2012/2012/11/06/d1c24c98-2802-11e2-b4e0-346287b7e56c_story.html");
        // pe.setDocument("http://mobile.smashingmagazine.com/2012/11/07/succeed-with-your-app/");
        // pe.setDocument("http://www.bbc.com/travel/feature/20121108-irelands-outlying-islands");
        // pe.setDocument("http://www.huffingtonpost.com/2012/11/22/black-friday-creep-retail-workers_n_2167066.html");
        // pe.setDocument("http://webknox.com/p/best-proxy-services");
        // pe.setDocument("http://www.politicususa.com/walmart-earns-record-profits-supporting-republicans-plan-slash-employees-food-stamps.html");
        // pe.setDocument("http://greatist.com/fitness/perfect-squat/");
        // pe.setDocument("http://www.latimes.com/news/nationworld/world/la-fg-israel-gaza-20121120,0,4042611.story");
        // pe.setDocument("http://www.labour.org.uk/govt-decision-on-palestine-vote-is-worse-than-a-blunder,2012-11-30");
        // pe.setDocument("C:\\Workspace\\data\\ContentExtraction\\TUD\\page203.html");
        // pe.setDocument("http://www.thedailybeast.com/articles/2012/11/29/where-ban-ki-moon-meets-pink-floyd.html");
        // pe.setDocument("http://www.nationalmemo.com/white-house-tax-rates-on-the-rich-will-go-up/");
        // pe.setDocument("http://www.ynetnews.com/articles/0,7340,L-4314175,00.html");
        // pe.setDocument("http://tech.slashdot.org/story/12/11/16/207227/german-city-says-openoffice-shortcomings-are-forcing-it-back-to-microsoft");
        // pe.setDocument("http://news.mongabay.com/2012/1204-hance-lions-population.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\82.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\105.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\771.html"); // ???
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\652.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\640.html");
        // pe.setDocument("http://www.upi.com/Top_News/US/2013/12/31/Man-faces-kidnapping-other-charges-in-trip-to-Las-Vegas-to-marry/UPI-67931388527587/");
        pe.setDocument("http://www.voanews.com/content/russia-urges-nations-to-take-active-role-in-the-middle-east-93610219/169955.html");

        // CollectionHelper.print(pe.setDocument("http://www.bbc.co.uk/news/science-environment-12209801").getImages());
        System.out.println("Title: " + pe.getResultTitle());
        // System.out.println("Author: "
        // + pe.getAuthorName(ConfigHolder.getInstance().getConfig().getString("api.webknox.apiKey")));
        System.out.println("Result Text: " + pe.getResultText());
        System.out.println("Comments: ");
        CollectionHelper.print(pe.getComments());

        System.out.println("Full Text: " + pe.getEntireTextContent());
        // CollectionHelper.print(pe.getSentences());
    }

}